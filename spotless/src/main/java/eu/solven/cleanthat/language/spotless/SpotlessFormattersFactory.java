package eu.solven.cleanthat.language.spotless;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.Provisioner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.resource.CleanthatUrlLoader;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.LanguageProperties;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.spotless.FormatterFactory;
import eu.solven.cleanthat.spotless.SpotlessProperties;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Formatter for Spotless Engine
 *
 * @author Benoit Lacelle
 */
public class SpotlessFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpotlessFormattersFactory.class);

	public SpotlessFormattersFactory(ConfigHelpers configHelpers) {
		super(configHelpers);
	}

	@Override
	public String getLanguage() {
		return "spotless";
	}

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("java", "scala", "json");
	}

	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		ILintFixerWithId processor;
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		// override with explicit configuration
		Map<String, ?> parameters = getParameters(rawProcessor);

		LOGGER.debug("Processing: {}", engine);

		switch (engine) {
		case "spotless": {
			SpotlessCleanthatProperties processorConfig = convertValue(parameters, SpotlessCleanthatProperties.class);

			String spotlessConfig = processorConfig.getConfiguration();
			if (Strings.isNullOrEmpty(spotlessConfig)) {
				throw new IllegalArgumentException("'configuration' is mandatory");
			}

			Resource spotlessPropertiesResource = CleanthatUrlLoader.loadUrl(codeProvider, spotlessConfig);

			SpotlessProperties spotlessProperties;
			try {
				spotlessProperties = getConfigHelpers().getObjectMapper()
						.readValue(spotlessPropertiesResource.getInputStream(), SpotlessProperties.class);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue with " + spotlessPropertiesResource, e);
			}

			Formatter formatter =
					new FormatterFactory(codeProvider).makeFormatter(spotlessProperties, makeProvisionner());

			processor = new SpotlessLintFixer(formatter);
			break;
		}

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		if (!processor.getId().equals(engine)) {
			throw new IllegalStateException("Inconsistency: " + processor.getId() + " vs " + engine);
		}

		return processor;
	}

	protected Provisioner makeProvisionner() {
		return FormatterFactory.makeProvisionner();
	}

	@Override
	public LanguageProperties makeDefaultProperties() {
		LanguageProperties languageProperties = new LanguageProperties();

		languageProperties.setLanguage(getLanguage());

		List<Map<String, ?>> processors = new ArrayList<>();

		// Apply rules
		{
			SpotlessCleanthatProperties engineParameters = new SpotlessCleanthatProperties();

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, "spotless")
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}

		languageProperties.setProcessors(processors);

		return languageProperties;
	}

}
