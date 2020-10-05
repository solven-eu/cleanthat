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
import eu.solven.cleanthat.github.IStringFormatter;
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
		properties.getLanguages().forEach(languageConfig -> {
			List<?> files;
			try {
				files = pr.listFiles();
			} catch (IOException e) {
				throw new UncheckedIOException("Issue listing files", e);
			}

			String language = PepperMapHelper.getRequiredString(languageConfig, "language");
			ILanguageProperties languageP =
					objectMapper.convertValue(languageConfig, CleanthatLanguageProperties.class);

			LOGGER.info("Applying includes rules: {}", languageP.getIncludes());
			LOGGER.info("Applying excludes rules: {}", languageP.getExcludes());

			AtomicLongMap<String> languageCounters = AtomicLongMap.create();

			files.forEach(file -> {
				if (pr.fileIsRemoved(file)) {
					// Skip files deleted within PR
					return;
				}

				String fileName = pr.getFilePath(file);

				Optional<PathMatcher> matchingInclude = findMatching(fileName, languageP.getIncludes());
				Optional<PathMatcher> matchingExclude = findMatching(fileName, languageP.getExcludes());

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
