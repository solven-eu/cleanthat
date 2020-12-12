package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AtomicLongMap;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.ISourceCodeProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.ICodeProvider;

/**
 * Unclear what is the point of this class
 *
 * @author Benoit Lacelle
 */
public class CodeProviderFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeProviderFormatter.class);

	public static final String EOL = "\r\n";

	final ObjectMapper objectMapper;

	final IStringFormatter formatter;

	public CodeProviderFormatter(ObjectMapper objectMapper, IStringFormatter formatter) {
		this.objectMapper = objectMapper;
		this.formatter = formatter;
	}

	public Map<String, ?> formatPR(CleanthatRepositoryProperties repoProperties, ICodeProvider pr) {
		// A config change may be cleanthat.json

		// TODO or an indirect change leading to a full re-compute (e.g. a implicit
		// version upgrade led to a change of some engine, which should trigger a full re-compute)
		AtomicBoolean configIsChanged = new AtomicBoolean();

		List<String> prComments = new ArrayList<>();

		try {
			pr.listFiles(fileChanged -> {
				if (GithubPullRequestCleaner.PATH_CLEANTHAT_JSON.equals(pr.getFilePath(fileChanged))) {
					configIsChanged.set(true);
					prComments.add("Configuration has changed");
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Issue while checking for config change", e);
		}

		AtomicLongMap<String> languageToNbAddedFiles = AtomicLongMap.create();
		AtomicLongMap<String> languagesCounters = AtomicLongMap.create();
		Map<String, String> pathToMutatedContent = new LinkedHashMap<>();
		repoProperties.getLanguages().forEach(dirtyLanguageConfig -> {
			ILanguageProperties languageP = prepareLanguageConfiguration(repoProperties, dirtyLanguageConfig);

			// TODO Process all languages in a single pass
			AtomicLongMap<String> languageCounters =
					countFiles(pr, languageToNbAddedFiles, pathToMutatedContent, languageP);
			String details = languageCounters.asMap()
					.entrySet()
					.stream()
					.map(e -> e.getKey() + ": " + e.getValue())
					.collect(Collectors.joining(EOL));

			prComments.add("language=" + languageP.getLanguage() + EOL + details);
			languageCounters.asMap().forEach((l, c) -> {
				languagesCounters.addAndGet(l, c);
			});
		});
		if (languageToNbAddedFiles.isEmpty() && !configIsChanged.get()) {
			LOGGER.info("Not a single file to commit ({})", pr.getHtmlUrl());
		} else if (configIsChanged.get()) {
			LOGGER.info("(Config change) About to check and possibly commit any files into {} ({})",
					pr.getHtmlUrl(),
					pr.getTitle());
			pr.commitIntoPR(pathToMutatedContent, prComments);
		} else {
			LOGGER.info("(No config change) About to check and possibly commit {} files into {} ({})",
					languageToNbAddedFiles.sum(),
					pr.getHtmlUrl(),
					pr.getTitle());
			pr.commitIntoPR(pathToMutatedContent, prComments);
		}
		return new LinkedHashMap<>(languagesCounters.asMap());
	}

	private ILanguageProperties prepareLanguageConfiguration(CleanthatRepositoryProperties repoProperties,
			Map<String, ?> dirtyLanguageConfig) {
		ConfigHelpers configHelpers = new ConfigHelpers(objectMapper);

		ILanguageProperties languageP = configHelpers.mergeLanguageProperties(repoProperties, dirtyLanguageConfig);

		String language = languageP.getLanguage();
		LOGGER.info("About to prepare files for language: {}", language);

		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();
		List<String> includes = languageP.getSourceCodeProperties().getIncludes();
		if (includes.isEmpty()) {
			if ("java".equals(languageP.getLanguage())) {
				List<String> defaultIncludes = Arrays.asList("glob:**/*.java");
				LOGGER.info("Default includes to: {}", defaultIncludes);
				// https://github.com/spring-io/spring-javaformat/blob/master/spring-javaformat-maven/spring-javaformat-maven-plugin/...
				// .../src/main/java/io/spring/format/maven/FormatMojo.java#L47
				languageP = configHelpers.forceIncludes(languageP, defaultIncludes);
				sourceCodeProperties = languageP.getSourceCodeProperties();
				includes = languageP.getSourceCodeProperties().getIncludes();
			} else {
				LOGGER.warn("No includes and no default for language={}", language);
			}
		}

		LOGGER.info("Applying includes rules: {}", sourceCodeProperties.getIncludes());
		LOGGER.info("Applying excludes rules: {}", sourceCodeProperties.getExcludes());
		return languageP;
	}

	private AtomicLongMap<String> countFiles(ICodeProvider pr,
			AtomicLongMap<String> languageToNbAddedFiles,
			Map<String, String> pathToMutatedContent,
			ILanguageProperties languageP) {
		String language = languageP.getLanguage();
		ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();

		AtomicLongMap<String> languageCounters = AtomicLongMap.create();
		try {
			pr.listFiles(file -> {
				if (pr.fileIsRemoved(file)) {
					// Skip files deleted within PR
					return;
				}
				String fileName = pr.getFilePath(file);
				Optional<PathMatcher> matchingInclude = findMatching(fileName, sourceCodeProperties.getIncludes());
				Optional<PathMatcher> matchingExclude = findMatching(fileName, sourceCodeProperties.getExcludes());
				if (matchingInclude.isPresent()) {
					if (matchingExclude.isEmpty()) {
						try {
							Optional<String> optAlreadyMutated =
									Optional.ofNullable(pathToMutatedContent.get(fileName));
							String code = optAlreadyMutated.orElseGet(() -> {
								try {
									return pr.loadContent(file);
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
							});
							LOGGER.info("Processing {}", fileName);
							String output = doFormat(languageP, code);
							if (!Strings.isNullOrEmpty(output) && !code.equals(output)) {
								pathToMutatedContent.put(fileName, output);
								languageToNbAddedFiles.incrementAndGet(language);
								languageCounters.incrementAndGet("nb_files_formatted");
							} else {
								languageCounters.incrementAndGet("nb_files_already_formatted");
							}
						} catch (IOException e) {
							throw new UncheckedIOException("Issue with file: " + fileName, e);
						} catch (RuntimeException e) {
							throw new RuntimeException("Issue with file: " + fileName, e);
						}
					} else {
						languageCounters.incrementAndGet("nb_files_both_included_excluded");
					}
				} else if (matchingExclude.isEmpty()) {
					languageCounters.incrementAndGet("nb_files_excluded_not_included");
				} else {
					languageCounters.incrementAndGet("nb_files_neither_included_nor_included");
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Issue listing files", e);
		}
		return languageCounters;
	}

	// https://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
	private Optional<PathMatcher> findMatching(String fileName, List<String> regex) {
		return regex.stream()
				.map(r -> FileSystems.getDefault().getPathMatcher(r))
				.filter(pm -> pm.matches(Paths.get(fileName)))
				.findFirst();
	}

	private String doFormat(ILanguageProperties properties, String code) throws IOException {
		return formatter.format(properties, code);
	}
}
