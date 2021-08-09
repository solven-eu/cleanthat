package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.nio.file.PathMatcher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.language.CleanthatLanguageProperties;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.ISourceCodeProperties;
import eu.solven.cleanthat.language.IStringFormatter;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
public abstract class ALanguageFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ALanguageFormatter.class);

	final ObjectMapper objectMapper;

	public ALanguageFormatter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	@Override
	public String format(ILanguageProperties languageProperties, String filepath, String code) throws IOException {
		AtomicReference<String> outputRef = new AtomicReference<>(code);

		// TODO Is this really a deep-copy?
		Map<String, ?> languagePropertiesTemplate =
				ImmutableMap.copyOf(objectMapper.convertValue(languageProperties, Map.class));

		languageProperties.getProcessors().forEach(rawProcessor -> {
			try {
				String input = outputRef.get();
				String output = applyProcessor(languagePropertiesTemplate, rawProcessor, filepath, input);
				if (output == null) {
					throw new IllegalStateException("Null code. TODO");
				}
				if (!input.equals(output)) {
					// Beware each processor may change a file, but the combined changes leads to a no change (e.g. the
					// final formatting step clean all previous not relevant changes)
					LOGGER.info("{} mutated a file", rawProcessor);
				}
				outputRef.set(output);
			} catch (IOException | RuntimeException e) {
				// Log and move to next processor
				LOGGER.warn("Issue with " + rawProcessor, e);
			}
		});
		return outputRef.get();
	}

	protected String applyProcessor(Map<String, ?> languagePropertiesTemplate,
			Map<String, ?> rawProcessor,
			String filepath,
			String code) throws IOException {
		Objects.requireNonNull(code, "code should not be null");
		Map<String, Object> languagePropertiesAsMap = new LinkedHashMap<>(languagePropertiesTemplate);
		// As we are processing a single processor, we can get ride of the processors field
		languagePropertiesAsMap.remove("processors");
		// An processor may need to be applied with an override languageVersion
		Map<String, ?> languageVersionOverload = PepperMapHelper.getAs(rawProcessor, "language_version");
		if (languageVersionOverload != null) {
			languagePropertiesAsMap.put("language_version", languageVersionOverload);
		}
		Map<String, ?> sourceOverloads = PepperMapHelper.getAs(rawProcessor, "source_code");
		if (sourceOverloads != null) {
			// Mutable copy
			Map<String, Object> sourcePropertiesAsMap =
					new LinkedHashMap<>(PepperMapHelper.getAs(languagePropertiesAsMap, "source_code"));
			// Mutate
			sourcePropertiesAsMap.putAll(sourceOverloads);
			// Re-inject
			languagePropertiesAsMap.put("source_code", sourcePropertiesAsMap);
		}
		ILanguageProperties languageProperties =
				objectMapper.convertValue(languagePropertiesAsMap, CleanthatLanguageProperties.class);
		ISourceCodeFormatter processor = makeFormatter(rawProcessor, languageProperties);
		ISourceCodeProperties sourceCodeProperties = languageProperties.getSourceCode();
		List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getIncludes());
		List<PathMatcher> excludeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getExcludes());
		Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filepath);
		Optional<PathMatcher> matchingExclude = IncludeExcludeHelpers.findMatching(excludeMatchers, filepath);
		if (matchingInclude.isEmpty()) {
			LOGGER.debug("File {} was initially included but not included for processor: {}", filepath, processor);
			return code;
		} else if (matchingExclude.isPresent()) {
			LOGGER.debug("File {} was initially not-excluded but excluded for processor: {}", filepath, processor);
			return code;
		}
		LineEnding lineEnding = languageProperties.getSourceCode().getLineEndingAsEnum();
		return processor.doFormat(code, lineEnding);
	}

	protected abstract ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties);
}
