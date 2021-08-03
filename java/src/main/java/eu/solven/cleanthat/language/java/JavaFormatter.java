package eu.solven.cleanthat.language.java;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.ALanguageFormatter;
import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.github.EclipseJavaFormatterProcessorProperties;
import eu.solven.cleanthat.java.mutators.JavaRulesMutatorProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.IStringFormatter;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.language.java.imports.JavaRevelcImportsCleaner;
import eu.solven.cleanthat.language.java.imports.JavaRevelcImportsCleanerProperties;
import eu.solven.cleanthat.language.java.spring.SpringJavaFormatter;
import eu.solven.cleanthat.language.java.spring.SpringJavaFormatterProperties;

/**
 * Formatter for Java
 *
 * @author Benoit Lacelle
 */
public class JavaFormatter extends ALanguageFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormatter.class);

	private static final int DEFAULT_CACHE_SIZE = 16;

	// Prevents parsing/loading remote configuration on each parse
	// We expect a low number of different configurations
	// Beware this can lead to race-conditions/thread-safety issues into EclipseJavaFormatter
	final LoadingCache<Map.Entry<ILanguageProperties, EclipseJavaFormatterProcessorProperties>, EclipseJavaFormatter> configToEngine =
			CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build(CacheLoader.from(config -> {
				return new EclipseJavaFormatter(config.getKey(), config.getValue());
			}));

	public JavaFormatter(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "java";
	}

	@VisibleForTesting
	protected long getCacheSize() {
		return configToEngine.size();
	}

	@Override
	protected ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor, ILanguageProperties languageProperties) {
		ISourceCodeFormatter processor;
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");
		// override with explicit configuration
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, "parameters");
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}

		LOGGER.info("Processing: {}", LOGGER);

		ObjectMapper objectMapper = getObjectMapper();

		switch (engine) {
		case "eclipse_formatter": {
			EclipseJavaFormatterProcessorProperties processorConfig =
					objectMapper.convertValue(parameters, EclipseJavaFormatterProcessorProperties.class);
			processor = configToEngine.getUnchecked(Map.entry(languageProperties, processorConfig));
		}
			break;

		case "revelc_imports": {
			JavaRevelcImportsCleanerProperties processorConfig =
					objectMapper.convertValue(parameters, JavaRevelcImportsCleanerProperties.class);
			processor = new JavaRevelcImportsCleaner(languageProperties.getSourceCodeProperties(), processorConfig);
		}
			break;

		case "spring_formatter": {
			SpringJavaFormatterProperties processorConfig =
					objectMapper.convertValue(parameters, SpringJavaFormatterProperties.class);
			processor = new SpringJavaFormatter(languageProperties.getSourceCodeProperties(), processorConfig);
		}
			break;
		case "rules": {
			JavaRulesMutatorProperties processorConfig =
					objectMapper.convertValue(parameters, JavaRulesMutatorProperties.class);
			processor = new RulesJavaMutator(languageProperties, processorConfig);
		}
			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}
		return processor;
	}
}
