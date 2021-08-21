package eu.solven.cleanthat.formatter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.LanguagePropertiesAndBuildProcessors;

/**
 * Helps compiling CodeProcessors in the context of a repository
 * 
 * @author Benoit Lacelle
 *
 */
public class SourceCodeFormatterHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceCodeFormatterHelper.class);

	private final ObjectMapper objectMapper;

	public SourceCodeFormatterHelper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public LanguagePropertiesAndBuildProcessors compile(ILanguageProperties languageProperties,
			ICodeProvider codeProvider,
			ILanguageLintFixerFactory lintFixerFactory) {
		List<Map.Entry<ILanguageProperties, ILintFixer>> processors =
				languageProperties.getProcessors().stream().map(rawProcessor -> {
					ILanguageProperties mergedLanguageProperties =
							mergeLanguageProperties(languageProperties, rawProcessor);
					ILintFixer formatter =
							lintFixerFactory.makeLintFixer(rawProcessor, languageProperties, codeProvider);
					return Maps.immutableEntry(mergedLanguageProperties, formatter);
				}).collect(Collectors.toList());

		List<IStyleEnforcer> codeStyleFixer = processors.stream()
				.map(e -> e.getValue())
				.filter(lf -> lf instanceof IStyleEnforcer)
				.map(lf -> (IStyleEnforcer) lf)
				.collect(Collectors.toList());

		if (codeStyleFixer.isEmpty()) {
			LOGGER.warn("It is certainly unsafe not to have a single {}", IStyleEnforcer.class.getSimpleName());
		} else {
			int nbCodeStyleFormatter = codeStyleFixer.size();
			if (nbCodeStyleFormatter >= 2) {
				LOGGER.warn("It is unsual to have multiple {} ({})",
						IStyleEnforcer.class.getSimpleName(),
						nbCodeStyleFormatter);
			}

			IStyleEnforcer firstCodeStyleFormatter = codeStyleFixer.get(0);

			processors.stream()
					.map(e -> e.getValue())
					.filter(Predicates.instanceOf(ILintFixerHelpedByCodeStyleFixer.class))
					.map(lf -> (ILintFixerHelpedByCodeStyleFixer) lf)
					.forEach(lf -> {
						lf.registerCodeStyleFixer(firstCodeStyleFormatter);
					});
		}

		return new LanguagePropertiesAndBuildProcessors(processors);
	}

	protected ILanguageProperties mergeLanguageProperties(ILanguageProperties languagePropertiesTemplate,
			Map<String, ?> rawProcessor) {
		Map<String, Object> languagePropertiesAsMap = makeDeepCopy(languagePropertiesTemplate);
		// As we are processing a single processor, we can get ride of the processors field
		languagePropertiesAsMap.remove("processors");
		// An processor may need to be applied with an override languageVersion
		Optional<String> optLanguageVersionOverload =
				PepperMapHelper.getOptionalString(rawProcessor, "language_version");
		if (optLanguageVersionOverload.isPresent()) {
			languagePropertiesAsMap.put("language_version", optLanguageVersionOverload.get());
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

	public <T> Map<String, Object> makeDeepCopy(T languagePropertiesTemplate) {
		try {
			// We make a deep-copy before mutation
			byte[] serialized = objectMapper.writeValueAsBytes(languagePropertiesTemplate);
			Map<String, ?> fromJackson = objectMapper.readValue(serialized, Map.class);

			return new LinkedHashMap<>(fromJackson);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Issue with: " + languagePropertiesTemplate, e);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue with: " + languagePropertiesTemplate, e);
		}
	}
}
