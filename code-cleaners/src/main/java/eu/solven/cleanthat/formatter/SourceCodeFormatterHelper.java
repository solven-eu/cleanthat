package eu.solven.cleanthat.formatter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.ISourceCodeFormatterFactory;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.LanguagePropertiesAndBuildProcessors;

/**
 * Helps compiling CodeProcessors in the context of a repository
 * 
 * @author Benoit Lacelle
 *
 */
public class SourceCodeFormatterHelper {

	private final ObjectMapper objectMapper;

	public SourceCodeFormatterHelper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public LanguagePropertiesAndBuildProcessors compile(ILanguageProperties languageProperties,
			ICodeProvider codeProvider,
			ISourceCodeFormatterFactory languageFormatter) {
		// TODO Is this really a deep-copy?
		Map<String, ?> languagePropertiesTemplate =
				ImmutableMap.copyOf(objectMapper.convertValue(languageProperties, Map.class));

		return new LanguagePropertiesAndBuildProcessors(
				languageProperties.getProcessors().stream().map(rawProcessor -> {
					ILanguageProperties mergedLanguageProperties =
							mergeLanguageProperties(languagePropertiesTemplate, rawProcessor);
					ISourceCodeFormatter formatter =
							languageFormatter.makeFormatter(rawProcessor, languageProperties, codeProvider);
					return Maps.immutableEntry(mergedLanguageProperties, formatter);
				}).collect(Collectors.toList()));
	}

	protected ILanguageProperties mergeLanguageProperties(Map<String, ?> languagePropertiesTemplate,
			Map<String, ?> rawProcessor) {
		Map<String, Object> languagePropertiesAsMap = new LinkedHashMap<>(languagePropertiesTemplate);
		// As we are processing a single processor, we can get ride of the processors field
		languagePropertiesAsMap.remove("processors");
		// An processor may need to be applied with an override languageVersion
		Map<String, ?> languageVersionOverload = PepperMapHelper.getAs(rawProcessor, "language_version");
		if (languageVersionOverload != null) {
			languagePropertiesAsMap.put("language_version", languageVersionOverload);
		}
		Optional<Map<String, ?>> optSourceOverloads = PepperMapHelper.getOptionalAs(rawProcessor, "source_code");
		if (optSourceOverloads.isPresent()) {
			// Mutable copy
			Map<String, Object> sourcePropertiesAsMap =
					new LinkedHashMap<>(PepperMapHelper.getRequiredMap(languagePropertiesAsMap, "source_code"));
			// Mutate
			sourcePropertiesAsMap.putAll(optSourceOverloads.get());
			// Re-inject
			languagePropertiesAsMap.put("source_code", sourcePropertiesAsMap);
		}
		ILanguageProperties languageProperties =
				objectMapper.convertValue(languagePropertiesAsMap, LanguageProperties.class);
		return languageProperties;
	}
}
