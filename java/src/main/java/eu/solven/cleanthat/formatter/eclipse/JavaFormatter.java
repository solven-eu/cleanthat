package eu.solven.cleanthat.formatter.eclipse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.github.CleanthatLanguageProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.java.imports.JavaRevelcImportsCleaner;
import eu.solven.cleanthat.java.imports.JavaRevelcImportsCleanerProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;

/**
 * Formatter for Java
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormatter.class);

	private static final int DEFAULT_CACHE_SIZE = 16;

	final ObjectMapper objectMapper;

	// Prevents parsing/loading remote configuration on each parse
	// We expect a low number of different configurations
	final LoadingCache<CleanthatJavaProcessorProperties, EclipseJavaFormatter> configToEngine =
			CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build(CacheLoader.from(config -> {
				return new EclipseJavaFormatter(config);
			}));

	public JavaFormatter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String format(ILanguageProperties languageProperties, String asString) throws IOException {
		Map<String, ?> languagePropertiesTemplate =
				ImmutableMap.copyOf(objectMapper.convertValue(languageProperties, Map.class));

		AtomicReference<String> outputRef = new AtomicReference<>(asString);
		languageProperties.getProcessors().forEach(pAsMap -> {
			Map<String, Object> languagePropertiesAsMap = new LinkedHashMap<>(languagePropertiesTemplate);

			Map<String, ?> languageOverload = PepperMapHelper.getAs(pAsMap, "language");
			if (languageOverload != null) {
				languagePropertiesAsMap.put("language", languageOverload);
			}
			Map<String, ?> languageVersionOverload = PepperMapHelper.getAs(pAsMap, "language_version");
			if (languageVersionOverload != null) {
				languagePropertiesAsMap.put("language_version", languageVersionOverload);
			}

			Map<String, ?> sourceOverloads = PepperMapHelper.getAs(pAsMap, "source_code");
			if (sourceOverloads != null) {
				Map<String, Object> sourcePropertiesAsMap =
						PepperMapHelper.getAs(languagePropertiesAsMap, "source_code");
				sourcePropertiesAsMap.putAll(sourceOverloads);
			}

			ILanguageProperties languagePropertiesForProcessor =
					objectMapper.convertValue(languagePropertiesAsMap, CleanthatLanguageProperties.class);

			ICodeProcessor processor;
			String engine = PepperMapHelper.getRequiredString(pAsMap, "engine");

			// override with explicit configuration
			Map<String, Object> parameters = PepperMapHelper.getAs(pAsMap, "parameters");
			if (parameters == null) {
				// Some engine takes no parameter
				parameters = Map.of();
			}

			if ("eclipse_formatter".equals(engine)) {
				CleanthatJavaProcessorProperties processorConfig =
						objectMapper.convertValue(parameters, CleanthatJavaProcessorProperties.class);
				processor = configToEngine.getUnchecked(processorConfig);
			} else if ("revelc_imports".equals(engine)) {
				JavaRevelcImportsCleanerProperties processorConfig =
						objectMapper.convertValue(parameters, JavaRevelcImportsCleanerProperties.class);

				processor = new JavaRevelcImportsCleaner(languagePropertiesForProcessor.getSourceCodeProperties(),
						processorConfig);
			} else if ("rules".equals(engine)) {
				CleanthatJavaProcessorProperties processorConfig =
						objectMapper.convertValue(parameters, CleanthatJavaProcessorProperties.class);
				processor = new RulesJavaMutator(processorConfig);
			} else {
				throw new IllegalArgumentException("Unknown engine: " + engine);
			}

			try {
				LineEnding lineEnding = languagePropertiesForProcessor.getSourceCodeProperties().getLineEnding();
				String input = outputRef.get();
				String output = processor.doFormat(input, lineEnding);
				if (!input.equals(output)) {
					LOGGER.info("{} mutated a file", engine);
				}
				outputRef.set(output);
			} catch (IOException | RuntimeException e) {
				// Log and move to next processor
				LOGGER.warn("Issue with " + processor, e);
			}
		});

		return outputRef.getAcquire();
	}

}
