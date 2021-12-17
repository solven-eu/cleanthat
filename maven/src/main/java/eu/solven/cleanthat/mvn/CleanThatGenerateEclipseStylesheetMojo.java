package eu.solven.cleanthat.mvn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.language.CleanthatUrlLoader;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.java.JavaFormattersFactory;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.language.java.eclipse.checkstyle.XmlProfileWriter;
import eu.solven.cleanthat.language.java.eclipse.generator.EclipseStylesheetGenerator;
import eu.solven.cleanthat.language.java.eclipse.generator.IEclipseStylesheetGenerator;
import eu.solven.cleanthat.utils.ResultOrError;

/**
 * The mojo generates an Eclipse formatter stylesheet minimyzing modifications over existing codebase.
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
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
			defaultValue = "${maven.multiModuleProjectDirectory}/.cleanthat/eclipse_formatter-stylesheet.xml")
	private String eclipseConfigPath;

	// Generate the stylesheet can be very slow: make it faster by considering a subset of files
	// e.g. -Djava.regex=MyClass\.java
	@Parameter(property = "java.regex", defaultValue = DEFAULT_JAVA_REGEX)
	private String javaRegex;

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
		return Arrays.asList(EclipseStylesheetGenerator.class);
	}

	@Override
	public void doClean(ApplicationContext appContext) throws IOException, MojoFailureException {
		IEclipseStylesheetGenerator generator = appContext.getBean(IEclipseStylesheetGenerator.class);

		Map<Path, String> pathToContent = loadAnyJavaFile(generator);

		Map<String, String> settings = generator.generateSettings(pathToContent);
		Path eclipseConfigPath = writeSettings(settings);

		String rawConfigPath = getConfigPath();
		if (Strings.isNullOrEmpty(rawConfigPath)) {
			LOGGER.warn("There is no configPath: please adjust your cleanthat.yaml manually, in order to rely on '{}'",
					eclipseConfigPath);
			return;
		}

		LOGGER.warn("About to inject '{}' into '{}'", eclipseConfigPath, rawConfigPath);
		injectStylesheetInConfig(appContext, eclipseConfigPath, rawConfigPath);
	}

	public void injectStylesheetInConfig(ApplicationContext appContext, Path eclipseConfigPath, String rawConfigPath)
			throws MojoFailureException {
		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);
		MavenCodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);
		ResultOrError<CleanthatRepositoryProperties, String> optResult =
				codeCleaner.loadAndCheckConfiguration(codeProvider);

		if (optResult.getOptError().isPresent()) {
			String error = optResult.getOptError().get();
			throw new MojoFailureException("ARG", error, error);
		}

		CleanthatRepositoryProperties loadedConfig = optResult.getOptResult().get();

		Optional<LanguageProperties> optJavaProperties =
				loadedConfig.getLanguages().stream().filter(lp -> "java".equals(lp.getLanguage())).findAny();

		List<ObjectMapper> objectMappers =
				appContext.getBeansOfType(ObjectMapper.class).values().stream().collect(Collectors.toList());
		ObjectMapper yamlObjectMapper = ConfigHelpers.getYaml(objectMappers);

		LanguageProperties javaProperties = optJavaProperties.orElseGet(() -> {
			// There is no java language properties
			LOGGER.info("We introduce the java language properties");

			// Enable mutations
			List<LanguageProperties> mutableLanguages = new ArrayList<>(loadedConfig.getLanguages());
			loadedConfig.setLanguages(mutableLanguages);

			LanguageProperties languageProperties = new JavaFormattersFactory(yamlObjectMapper).makeDefaultProperties();
			mutableLanguages.add(languageProperties);

			return languageProperties;
		});

		Optional<Map<String, ?>> optEclipseProperties = javaProperties.getProcessors()
				.stream()
				.filter(p -> EclipseJavaFormatter.ID.equals(p.get("engine")))
				.findAny();

		Map<String, ?> eclipseProperties;
		if (optEclipseProperties.isPresent()) {
			eclipseProperties = optEclipseProperties.get();
		} else {
			eclipseProperties = JavaFormattersFactory.makeEclipseFormatterDefaultProperties();
			javaProperties.getProcessors().add(eclipseProperties);
		}

		Optional<Map<String, Object>> optEclipseParameters =
				PepperMapHelper.getOptionalAs(eclipseProperties, ILanguageLintFixerFactory.KEY_PARAMETERS);

		Map<String, Object> eclipseParameters = optEclipseParameters.orElse(new TreeMap<>());
		eclipseParameters.put("url", CleanthatUrlLoader.PREFIX_CODE + eclipseConfigPath);

		// Prepare the configuration as yaml
		String asYaml;
		try {
			asYaml = yamlObjectMapper.writeValueAsString(loadedConfig);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Issue converting " + loadedConfig + " to YAML", e);
		}

		// Write at given path
		try {
			Files.writeString(Paths.get(rawConfigPath), asYaml, Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue writing YAML into: " + rawConfigPath, e);
		}
	}

	protected Map<Path, String> loadAnyJavaFile(IEclipseStylesheetGenerator generator) {
		MavenProject mavenProject = getProject();

		List<MavenProject> collectedProjects = mavenProject.getCollectedProjects();

		Set<Path> roots;
		if (collectedProjects == null) {
			Path executionRoot = getBaseDir().toPath();
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
				return Stream.concat(sourceRoots.stream(), testRoots.stream())
						.map(sourceFolder -> projectBaseDir.resolve(sourceFolder));
			}).collect(Collectors.toSet());
		}

		return loadAnyJavaFile(generator, roots);
	}

	protected Map<Path, String> loadAnyJavaFile(IEclipseStylesheetGenerator generator, Set<Path> roots) {
		Map<Path, String> pathToContent = new LinkedHashMap<>();
		roots.forEach(rootAsPath -> {
			try {
				if (!rootAsPath.toFile().exists()) {
					LOGGER.debug("The root folder '{}' does not exist", rootAsPath);
					return;
				}

				Pattern compiledRegex = Pattern.compile(javaRegex);
				Map<Path, String> fileToContent = generator.loadFilesContent(rootAsPath, compiledRegex);
				LOGGER.info("Loaded {} files from {}", fileToContent.size(), rootAsPath);
				pathToContent.putAll(fileToContent);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return pathToContent;
	}

	protected Path writeSettings(Map<String, String> settings) throws IOException {
		Path whereToWrite = Paths.get(eclipseConfigPath);
		File whereToWriteAsFile = whereToWrite.toFile().getAbsoluteFile();
		if (whereToWriteAsFile.exists()) {
			if (whereToWriteAsFile.isFile()) {
				LOGGER.warn("We are going to write over '{}'", whereToWrite);
			} else {
				throw new IllegalStateException("There is something but not a file at: " + whereToWriteAsFile);
			}
		} else {
			whereToWriteAsFile.getParentFile().mkdirs();
		}

		try (InputStream is = XmlProfileWriter.writeFormatterProfileToStream("cleanthat", settings);
				OutputStream outputStream = Files.newOutputStream(whereToWrite, StandardOpenOption.CREATE)) {
			ByteStreams.copy(is, outputStream);
		} catch (TransformerException | ParserConfigurationException e) {
			throw new IllegalArgumentException(e);
		}

		return whereToWrite;
	}
}