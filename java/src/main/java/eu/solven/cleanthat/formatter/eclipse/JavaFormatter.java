package eu.solven.cleanthat.formatter.eclipse;

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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.formatter.spring.SpringJavaFormatter;
import eu.solven.cleanthat.formatter.spring.SpringJavaFormatterProperties;
import eu.solven.cleanthat.github.CleanthatJavaProcessorProperties;
import eu.solven.cleanthat.github.CleanthatLanguageProperties;
import eu.solven.cleanthat.github.EclipseJavaFormatterProcessorProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.ISourceCodeProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.java.imports.JavaRevelcImportsCleaner;
import eu.solven.cleanthat.java.imports.JavaRevelcImportsCleanerProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;

/**
 * Formatter for Java
 *
 * @author Benoit Lacelle
 */
public class JavaFormatter implements IStringFormatter {

	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormatter.class);

	private static final int DEFAULT_CACHE_SIZE = 16;

	final ObjectMapper objectMapper;

	// Prevents parsing/loading remote configuration on each parse
	// We expect a low number of different configurations
	// Beware this can lead to race-conditions/thread-safety issues into EclipseJavaFormatter
	final LoadingCache<Map.Entry<ILanguageProperties, EclipseJavaFormatterProcessorProperties>, EclipseJavaFormatter> configToEngine =
			CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build(CacheLoader.from(config -> {
				return new EclipseJavaFormatter(config.getKey(), config.getValue());
			}));

	public JavaFormatter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@VisibleForTesting
	protected long getCacheSize() {
		return configToEngine.size();
	}

	@Override
	public String format(ILanguageProperties languageProperties, String filepath, String code) throws IOException {
		AtomicReference<String> outputRef = new AtomicReference<>(code);
		languageProperties.getProcessors().forEach(rawProcessor -> {
			// TODO Is this really a deep-copy?
			Map<String, ?> languagePropertiesTemplate =
					ImmutableMap.copyOf(objectMapper.convertValue(languageProperties, Map.class));

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

		ISourceCodeProperties sourceCodeProperties = languageProperties.getSourceCodeProperties();

		List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getIncludes());
		List<PathMatcher> excludeMatchers = IncludeExcludeHelpers.prepareMatcher(sourceCodeProperties.getExcludes());

		Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filepath);
		Optional<PathMatcher> matchingExclude = IncludeExcludeHelpers.findMatching(excludeMatchers, filepath);

		if (!matchingInclude.isPresent()) {
			LOGGER.info("File {} was initially included but not included for processor: {}", processor);
			return code;
		} else if (matchingExclude.isPresent()) {
			LOGGER.info("File {} was initially not-excluded but excluded for processor: {}", processor);
			return code;
		}

		LineEnding lineEnding = languageProperties.getSourceCodeProperties().getLineEnding();
		return processor.doFormat(code, lineEnding);
	}

	private ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor, ILanguageProperties languageProperties) {
		ISourceCodeFormatter processor;
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");

		// override with explicit configuration
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, "parameters");
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}

		if ("eclipse_formatter".equals(engine)) {
			EclipseJavaFormatterProcessorProperties processorConfig =
					objectMapper.convertValue(parameters, EclipseJavaFormatterProcessorProperties.class);
			processor = configToEngine.getUnchecked(Map.entry(languageProperties, processorConfig));
		} else if ("revelc_imports".equals(engine)) {
			JavaRevelcImportsCleanerProperties processorConfig =
					objectMapper.convertValue(parameters, JavaRevelcImportsCleanerProperties.class);
			processor = new JavaRevelcImportsCleaner(languageProperties.getSourceCodeProperties(), processorConfig);
		} else if ("spring_formatter".equals(engine)) {
			SpringJavaFormatterProperties processorConfig =
					objectMapper.convertValue(parameters, SpringJavaFormatterProperties.class);
			processor = new SpringJavaFormatter(languageProperties.getSourceCodeProperties(), processorConfig);
		} else if ("rules".equals(engine)) {
			CleanthatJavaProcessorProperties processorConfig =
					objectMapper.convertValue(parameters, CleanthatJavaProcessorProperties.class);
			processor = new RulesJavaMutator(languageProperties, processorConfig);
		} else {
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}
		return processor;
	}
}
