package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AtomicLongMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.CleanthatLanguageProperties;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.ISourceCodeProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.github.SourceCodeProperties;
import eu.solven.cleanthat.github.event.ICodeProvider;

/**
 * Unclear what is the point of this class
 * 
 * @author Benoit Lacelle
 *
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

	public Map<String, ?> formatPR(CleanthatRepositoryProperties properties, ICodeProvider pr) {
		AtomicLongMap<String> languageToNbAddedFiles = AtomicLongMap.create();
		AtomicLongMap<String> languagesCounters = AtomicLongMap.create();
		Map<String, String> pathToMutatedContent = new LinkedHashMap<>();

		List<String> prComments = new ArrayList<>();
		properties.getLanguages().forEach(dirtyLanguageConfig -> {
			ISourceCodeProperties sourceConfdig = mergeSourceConfig(properties, dirtyLanguageConfig);

			Map<String, Object> languageConfig = new LinkedHashMap<>();
			languageConfig.putAll(dirtyLanguageConfig);
			languageConfig.put("source_code", sourceConfdig);

			String language = PepperMapHelper.getRequiredString(dirtyLanguageConfig, "language");
			LOGGER.info("About to prepare files for language: {}", language);
			ILanguageProperties languageP =
					objectMapper.convertValue(languageConfig, CleanthatLanguageProperties.class);

			ISourceCodeProperties sourceCodeProperties = languageP.getSourceCodeProperties();
			LOGGER.info("Applying includes rules: {}", sourceCodeProperties.getIncludes());
			LOGGER.info("Applying excludes rules: {}", sourceCodeProperties.getExcludes());

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

			String details = languageCounters.asMap()
					.entrySet()
					.stream()
					.map(e -> e.getKey() + ": " + e.getValue())
					.collect(Collectors.joining(EOL));

			prComments.add("language=" + language + EOL + details);

			languageCounters.asMap().forEach((l, c) -> {
				languagesCounters.addAndGet(l, c);
			});
		});

		if (languageToNbAddedFiles.isEmpty()) {
			LOGGER.info("Not a single file to commit ({})", pr.getHtmlUrl());
		} else {
			LOGGER.info("About to commit {} files into {} ({})",
					languageToNbAddedFiles.sum(),
					pr.getHtmlUrl(),
					pr.getTitle());

			pr.commitIntoPR(pathToMutatedContent, prComments);
		}

		return new LinkedHashMap<>(languagesCounters.asMap());
	}

	private ISourceCodeProperties mergeSourceConfig(CleanthatRepositoryProperties properties,
			Map<String, ?> dirtyLanguageConfig) {
		Map<String, Object> sourceConfig = new LinkedHashMap<>();

		// Apply defaults from parent
		sourceConfig.putAll(objectMapper.convertValue(properties.getSourceCodeProperties(), Map.class));

		// Apply explicit configuration
		Map<String, ?> explicitSourceCodeProperties = PepperMapHelper.getAs(dirtyLanguageConfig, "source_code");
		if (explicitSourceCodeProperties != null) {
			sourceConfig.putAll(explicitSourceCodeProperties);
		}

		return objectMapper.convertValue(sourceConfig, SourceCodeProperties.class);
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
