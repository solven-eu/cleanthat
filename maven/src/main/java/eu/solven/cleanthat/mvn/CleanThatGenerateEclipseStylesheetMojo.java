/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.mvn;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.code_provider.github.CodeCleanerSpringConfig;
import eu.solven.cleanthat.codeprovider.CodeWritingMetadata;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.java.eclipse.checkstyle.XmlProfileWriter;
import eu.solven.cleanthat.engine.java.eclipse.generator.EclipseStylesheetGenerator;
import eu.solven.cleanthat.engine.java.eclipse.generator.IEclipseStylesheetGenerator;
import eu.solven.cleanthat.git.GitIgnoreParser;
import eu.solven.cleanthat.language.spotless.CleanthatSpotlessStepParametersProperties;
import eu.solven.cleanthat.language.spotless.SpotlessFormattersFactory;
import eu.solven.cleanthat.spotless.FormatterFactory;
import eu.solven.cleanthat.spotless.language.JavaFormatterFactory;
import eu.solven.cleanthat.spotless.language.JavaFormatterStepFactory;
import eu.solven.cleanthat.spotless.pojo.SpotlessEngineProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessFormatterProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepParametersProperties;
import eu.solven.cleanthat.spotless.pojo.SpotlessStepProperties;
import eu.solven.cleanthat.utils.ResultOrError;

/**
 * The mojo generates an Eclipse formatter stylesheet minimyzing modifications over existing codebase.
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@SuppressWarnings("PMD.GodClass")
@Mojo(name = CleanThatGenerateEclipseStylesheetMojo.GOAL_ECLIPSE,
		defaultPhase = LifecyclePhase.NONE,
		threadSafe = true,
		aggregator = true,
		requiresProject = false)
public class CleanThatGenerateEclipseStylesheetMojo extends ACleanThatSpringMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatCleanThatMojo.class);

	// ".*/src/[main|test]/java/.*/.*\\.java"
	public static final String DEFAULT_JAVA_REGEX = ".*\\.java";

	public static final String GOAL_ECLIPSE = "eclipse_formatter-stylesheet";

	// https://stackoverflow.com/questions/3084629/finding-the-root-directory-of-a-multi-module-maven-reactor-project
	@Parameter(property = "eclipse_formatter.url",
			// defaultValue = "${maven.multiModuleProjectDirectory}/.cleanthat/eclipse_formatter-stylesheet.xml"
			defaultValue = "${session.request.multiModuleProjectDirectory}"
					+ JavaFormatterStepFactory.DEFAULT_ECLIPSE_FILE)
	private String eclipseConfigPath;

	// Generate the stylesheet can be very slow: make it faster by considering a subset of files
	// e.g. -Djava.regex=MyClass\.java
	@Parameter(property = "java.regex", defaultValue = DEFAULT_JAVA_REGEX)
	private String javaRegex;

	// PnYnMnDTnHnMnS
	// https://en.wikipedia.org/wiki/ISO_8601#Durations
	@Parameter(property = "duration.limit", defaultValue = "PT1M")
	private String rawDurationLimit;

	@VisibleForTesting
	protected void setConfigPath(String eclipseConfigPath) {
		this.eclipseConfigPath = eclipseConfigPath;
	}

	@VisibleForTesting
	protected void setJavaRegex(String javaRegex) {
		this.javaRegex = javaRegex;
	}

	@Override
	protected List<? extends Class<?>> springClasses() {
		return Arrays.asList(EclipseStylesheetGenerator.class,
				// Used to parse the existing config, to inject Eclipse stylesheet
				CodeCleanerSpringConfig.class);
	}

	@Override
	public void doClean(ApplicationContext appContext) throws IOException, MojoFailureException {
		IEclipseStylesheetGenerator generator = appContext.getBean(IEclipseStylesheetGenerator.class);

		Map<Path, String> pathToContent = loadAnyJavaFile(generator);

		var durationLimit = Duration.parse(rawDurationLimit);
		var timeLimit = OffsetDateTime.now().plus(durationLimit);
		LOGGER.info(
				"Job is limitted to duration={} (can be adjusted with '-Dduration.limit=PT1M') It will end at most at: {}",
				rawDurationLimit,
				timeLimit);

		Map<String, String> settings = generator.generateSettings(timeLimit, pathToContent);
		var eclipseConfigPath = writeSettings(settings);

		// TODO In fact, we go through Spotless to do so
		Path cleanthatConfigPath = getMayNotExistRepositoryConfigPath();
		LOGGER.info("About to inject '{}' into '{}'", eclipseConfigPath, cleanthatConfigPath);

		// We expect configPath to be like '.../.cleanthat/cleanthat.yaml'
		var dotCleanthatFolder = cleanthatConfigPath.getParent();
		if (dotCleanthatFolder == null) {
			throw new IllegalArgumentException("Issue with configPath: " + cleanthatConfigPath + " (no root)");
		} else if (!".cleanthat".equals(dotCleanthatFolder.getFileName().toString())) {
			LOGGER.warn("The configuration is not in a '.cleanthat' parent folder. We skip injecting {} in {}",
					eclipseConfigPath,
					cleanthatConfigPath);
			return;
		} else if (!Files.exists(cleanthatConfigPath)) {
			LOGGER.info("We skip injecting {} as {} does not exists", eclipseConfigPath, cleanthatConfigPath);
			return;
		}

		var repositoryRoot = dotCleanthatFolder.getParent();
		if (repositoryRoot == null) {
			throw new IllegalArgumentException("Issue with configPath: " + cleanthatConfigPath + " (no root)");
		}

		injectStylesheetInConfig(appContext, repositoryRoot, cleanthatConfigPath, eclipseConfigPath);
	}

	/**
	 * This will generate an Eclipse stylesheet based on current repository, and persist its configuration into
	 * cleanthat+spotless configuration files
	 * 
	 * @param appContext
	 * @param repoRoot
	 * @param cleanthatConfigPath
	 * @param eclipseConfigPath
	 * @throws MojoFailureException
	 * @throws IOException
	 */
	public void injectStylesheetInConfig(ApplicationContext appContext,
			Path repoRoot,
			Path cleanthatConfigPath,
			Path eclipseConfigPath) throws MojoFailureException, IOException {
		LOGGER.info("You need to wire manually the Eclipse stylesheet path (e.g. into '/.cleanthat/spotless.yaml'");
		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);
		MavenCodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);
		ResultOrError<CleanthatRepositoryProperties, String> optResult =
				codeCleaner.loadAndCheckConfiguration(codeProvider);

		if (optResult.getOptError().isPresent()) {
			var error = optResult.getOptError().get();
			throw new MojoFailureException("ARG", error, error);
		}

		var repositoryProperties = optResult.getOptResult().get();

		Optional<CleanthatEngineProperties> optSpotlessProperties = repositoryProperties.getEngines()
				.stream()
				.filter(lp -> CleanthatSpotlessStepParametersProperties.ENGINE_ID.equals(lp.getEngine()))
				.findAny();

		boolean needToSaveCleanthat;

		CleanthatEngineProperties spotlessEngine;
		if (optSpotlessProperties.isEmpty()) {
			spotlessEngine = appContext.getBean(SpotlessFormattersFactory.class)
					.makeDefaultProperties(Set.of(FormatterFactory.ID_JAVA));

			repositoryProperties.setEngines(ImmutableList.<CleanthatEngineProperties>builder()
					.addAll(repositoryProperties.getEngines())
					.add(spotlessEngine)
					.build());

			LOGGER.info("Append Spotless engine into Cleanthat configuration");
			needToSaveCleanthat = true;
		} else {
			spotlessEngine = optSpotlessProperties.get();
			needToSaveCleanthat = false;
		}

		ConfigHelpers configHelpers = appContext.getBean(ConfigHelpers.class);
		var objectMapper = configHelpers.getObjectMapper();

		var spotlessParameters = spotlessEngine.getSteps().get(0).getParameters();
		var rawPathToSpotlessConfig =
				objectMapper.convertValue(spotlessParameters, CleanthatSpotlessStepParametersProperties.class)
						.getConfiguration();

		SpotlessEngineProperties spotlessEngineProperties =
				loadOrInitSpotlessEngineProperties(codeProvider, objectMapper, rawPathToSpotlessConfig);

		Optional<SpotlessFormatterProperties> optJavaFormatter = spotlessEngineProperties.getFormatters()
				.stream()
				.filter(f -> FormatterFactory.ID_JAVA.equals(f.getFormat()))
				.findFirst();

		SpotlessFormatterProperties javaFormatter;

		var needToSaveSpotless = false;
		if (optJavaFormatter.isEmpty()) {
			javaFormatter = SpotlessFormatterProperties.builder().format(FormatterFactory.ID_JAVA).build();
			spotlessEngineProperties.getFormatters().add(javaFormatter);

			LOGGER.info("Append java formatter into Spotless engine");
			needToSaveSpotless = true;
		} else {
			javaFormatter = optJavaFormatter.get();
		}

		SpotlessStepProperties eclipseStep;
		Optional<SpotlessStepProperties> optEclipseStep = javaFormatter.getSteps()
				.stream()
				.filter(f -> JavaFormatterStepFactory.ID_ECLIPSE.equalsIgnoreCase(f.getId()))
				.findFirst();
		if (optEclipseStep.isEmpty()) {
			eclipseStep = JavaFormatterFactory.makeDefaultEclipseStep();

			javaFormatter.setSteps(ImmutableList.<SpotlessStepProperties>builder()
					.addAll(javaFormatter.getSteps())
					.add(eclipseStep)
					.build());

			LOGGER.info("Append eclipse step into Java formatter");
			needToSaveSpotless = true;
		} else {
			eclipseStep = optEclipseStep.get();
		}

		SpotlessStepParametersProperties eclipseParameters = eclipseStep.getParameters();
		if (eclipseParameters == null) {
			eclipseParameters = JavaFormatterFactory.makeDefaultEclipseStep().getParameters();
			needToSaveSpotless = true;
		}

		String eclipseStylesheetFile =
				eclipseParameters.getCustomProperty(JavaFormatterStepFactory.KEY_ECLIPSE_FILE, String.class);
		if (eclipseStylesheetFile != null) {
			LOGGER.debug("TODO We should have written eclipse in this location");
		} else {
			eclipseParameters.putProperty(JavaFormatterStepFactory.KEY_ECLIPSE_FILE, eclipseConfigPath.toString());
			needToSaveSpotless = true;
		}

		if (needToSaveCleanthat) {
			var rawCleanthatConfigPath = CleanthatPathHelpers.makeContentRawPath(repoRoot, cleanthatConfigPath);
			var cleanthatConfigContentPath = CleanthatPathHelpers.makeContentPath(repoRoot, rawCleanthatConfigPath);
			persistConfigurationFiles(appContext, codeProvider, cleanthatConfigContentPath, repositoryProperties);
		}
		if (needToSaveSpotless) {
			var spotlessConfigContentPath = CleanthatPathHelpers.makeContentPath(repoRoot, rawPathToSpotlessConfig);
			persistConfigurationFiles(appContext, codeProvider, spotlessConfigContentPath, spotlessEngineProperties);
		}
	}

	private SpotlessEngineProperties loadOrInitSpotlessEngineProperties(ICodeProvider codeProvider,
			ObjectMapper objectMapper,
			String pathToSpotlessConfig) throws IOException {
		return loadSpotlessEngineProperties(codeProvider, objectMapper, pathToSpotlessConfig);
	}

	public static SpotlessEngineProperties loadSpotlessEngineProperties(ICodeProvider codeProvider,
			ObjectMapper objectMapper,
			String pathToSpotlessConfig) throws IOException {
		var spotlessConfigAsResource = CleanthatUrlLoader.loadUrl(codeProvider, pathToSpotlessConfig);

		try (var inputStream = spotlessConfigAsResource.getInputStream()) {
			return objectMapper.readValue(inputStream, SpotlessEngineProperties.class);
		}
	}

	private void persistConfigurationFiles(ApplicationContext appContext,
			ICodeProviderWriter codeWriter,
			Path configPath,
			Object properties) {
		List<ObjectMapper> objectMappers =
				appContext.getBeansOfType(ObjectMapper.class).values().stream().collect(Collectors.toList());
		var yamlObjectMapper = ConfigHelpers.getYaml(objectMappers);

		// Prepare the configuration as yaml
		String asYaml;
		try {
			asYaml = yamlObjectMapper.writeValueAsString(properties);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Issue converting " + properties + " to YAML", e);
		}

		codeWriter.persistChanges(Map.of(configPath, asYaml), CodeWritingMetadata.empty());
	}

	/**
	 * Convert an OS dependent path to a '/root/folder/file' String.
	 * 
	 * @param path
	 * @return
	 */
	public static String toString(Path path) {
		return Streams.stream(path.iterator())
				.map(p -> p.getFileName().toString())
				.collect(Collectors.joining("/", "/", ""));
	}

	protected Map<Path, String> loadAnyJavaFile(IEclipseStylesheetGenerator generator) {
		MavenProject mavenProject = getProject();

		List<MavenProject> collectedProjects = mavenProject.getCollectedProjects();

		Path executionRoot = getBaseDir().toPath();
		Set<String> gitIgnorePatterns = loadGitIgnorePatterns(executionRoot);

		Set<Path> roots;
		if (collectedProjects == null) {
			LOGGER.info("Processing a folder with no 'pom.xml'. We will then process anything in '{}' matching '{}'",
					executionRoot,
					javaRegex);
			roots = Collections.singleton(executionRoot);
		} else {
			roots = collectedProjects.stream().flatMap(p -> {
				Path projectBaseDir = p.getBasedir().toPath();
				List<String> sourceRoots = p.getCompileSourceRoots();
				List<String> testRoots = p.getTestCompileSourceRoots();

				LOGGER.debug("Consider for baseDir '{}': {} and {}", projectBaseDir, sourceRoots, testRoots);

				// We make path relative to the baseDir, even through it seems mvn provides absolute paths is default
				// case
				return Stream.concat(sourceRoots.stream(), testRoots.stream()).map(projectBaseDir::resolve);
			}).collect(Collectors.toSet());
		}

		return loadAnyJavaFile(gitIgnorePatterns, generator, roots);
	}

	protected Set<String> loadGitIgnorePatterns(Path executionRoot) {
		// TODO This assumes the command is run from the repository root
		var gitIgnore = executionRoot.resolve(".gitignore");
		var gitIgnoreFile = gitIgnore.toFile();

		Set<String> gitIgnorePatterns;
		if (gitIgnoreFile.isFile()) {
			LOGGER.info("We detected a .gitignore ({})", gitIgnore);

			String gitIgnoreContent;
			try {
				gitIgnoreContent =
						new String(ByteStreams.toByteArray(new FileSystemResource(gitIgnoreFile).getInputStream()),
								StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue loading: " + gitIgnore, e);
			}
			gitIgnorePatterns = GitIgnoreParser.parsePatterns(gitIgnoreContent);
		} else {
			gitIgnorePatterns = Collections.emptySet();
		}
		return gitIgnorePatterns;
	}

	protected Map<Path, String> loadAnyJavaFile(Set<String> gitIgnorePatterns,
			IEclipseStylesheetGenerator generator,
			Set<Path> roots) {
		Map<Path, String> pathToContent = new LinkedHashMap<>();

		var nbFilteredByGitignore = new AtomicInteger();

		roots.forEach(rootAsPath -> {
			try {
				if (!rootAsPath.toFile().exists()) {
					LOGGER.debug("The root folder '{}' does not exist", rootAsPath);
					return;
				}

				var compiledRegex = Pattern.compile(javaRegex);
				Map<Path, String> fileToContent = generator.loadFilesContent(rootAsPath, compiledRegex);

				if (!gitIgnorePatterns.isEmpty()) {
					// Enable mutability
					Map<Path, String> gitIgnoreFiltered = new HashMap<>(fileToContent);

					gitIgnoreFiltered.keySet().removeIf(path -> !GitIgnoreParser.match(gitIgnorePatterns, path));

					nbFilteredByGitignore.addAndGet(fileToContent.size() - gitIgnoreFiltered.size());
					fileToContent = gitIgnoreFiltered;
				}

				LOGGER.info("Loaded {} files from {}", fileToContent.size(), rootAsPath);

				if (!gitIgnorePatterns.isEmpty()) {
					LOGGER.info("#files ignored by .gitignore: {}", nbFilteredByGitignore);
				}

				pathToContent.putAll(fileToContent);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return pathToContent;
	}

	protected Path writeSettings(Map<String, String> settings) throws IOException {
		if (eclipseConfigPath.startsWith("${")) {
			throw new IllegalArgumentException("Issue with mvn placeholders: " + eclipseConfigPath);
		}
		var whereToWrite = Paths.get(eclipseConfigPath);

		var whereToWriteAsFile = whereToWrite.toFile().getAbsoluteFile();
		if (whereToWriteAsFile.exists()) {
			if (whereToWriteAsFile.isFile()) {
				LOGGER.warn("We are going to write over '{}'", whereToWrite);
			} else {
				throw new IllegalStateException("There is something but not a file/folder at: " + whereToWriteAsFile);
			}
		} else {
			LOGGER.info("About to write Eclipse configuration at: {}", whereToWriteAsFile);
			if (whereToWriteAsFile.getParentFile().mkdirs()) {
				LOGGER.info(".mkdirs() successful on {}", whereToWriteAsFile.getParentFile());
			}
		}

		try (InputStream is = XmlProfileWriter.writeFormatterProfileToStream("cleanthat", settings);
				var outputStream = Files.newOutputStream(whereToWrite, StandardOpenOption.CREATE)) {
			ByteStreams.copy(is, outputStream);
		} catch (TransformerException | ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}

		return whereToWrite;
	}
}