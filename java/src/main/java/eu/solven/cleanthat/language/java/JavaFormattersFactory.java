package eu.solven.cleanthat.language.java;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.java.mutators.JavaRulesMutatorProperties;
import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatter;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatterConfiguration;
import eu.solven.cleanthat.language.java.eclipse.EclipseJavaFormatterProcessorProperties;
import eu.solven.cleanthat.language.java.imports.JavaRevelcImportsCleaner;
import eu.solven.cleanthat.language.java.imports.JavaRevelcImportsCleanerProperties;
import eu.solven.cleanthat.language.java.spring.SpringJavaFormatterProperties;
import eu.solven.cleanthat.language.java.spring.SpringJavaStyleEnforcer;

/**
 * Formatter for Java
 *
 * @author Benoit Lacelle
 */
public class JavaFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaFormattersFactory.class);

	private static final int DEFAULT_CACHE_SIZE = 16;

	// Prevents parsing/loading remote configuration on each parse
	// We expect a low number of different configurations
	// Beware this can lead to race-conditions/thread-safety issues into EclipseJavaFormatter
	final Cache<EclipseFormatterCacheKey, EclipseJavaFormatterConfiguration> configToEngine =
			CacheBuilder.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build();

	public JavaFormattersFactory(ObjectMapper objectMapper) {
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
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		ILintFixer processor;
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");
		// override with explicit configuration
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, "parameters");
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}

		LOGGER.debug("Processing: {}", engine);

		ObjectMapper objectMapper = getObjectMapper();

		switch (engine) {
		case "eclipse_formatter": {
			EclipseJavaFormatterProcessorProperties processorConfig =
					objectMapper.convertValue(parameters, EclipseJavaFormatterProcessorProperties.class);
			EclipseJavaFormatterConfiguration configuration;
			try {
				configuration = configToEngine
						.get(new EclipseFormatterCacheKey(codeProvider, languageProperties, processorConfig), () -> {
							return EclipseJavaFormatterConfiguration
									.load(codeProvider, languageProperties, processorConfig);
						});
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
			processor = new EclipseJavaFormatter(configuration);
		}
			break;

		case "revelc_imports": {
			JavaRevelcImportsCleanerProperties processorConfig =
					objectMapper.convertValue(parameters, JavaRevelcImportsCleanerProperties.class);
			processor = new JavaRevelcImportsCleaner(languageProperties.getSourceCode(), processorConfig);
		}
			break;

		case "spring_formatter": {
			SpringJavaFormatterProperties processorConfig =
					objectMapper.convertValue(parameters, SpringJavaFormatterProperties.class);
			processor = new SpringJavaStyleEnforcer(languageProperties.getSourceCode(), processorConfig);
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
