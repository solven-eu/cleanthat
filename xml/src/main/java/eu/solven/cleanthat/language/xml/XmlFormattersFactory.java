package eu.solven.cleanthat.language.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.xml.jackson.JacksonXmlFormatter;
import eu.solven.cleanthat.language.xml.jackson.JacksonXmlFormatterProperties;

/**
 * Formatter for Json
 *
 * @author Benoit Lacelle
 */
public class XmlFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlFormattersFactory.class);

	public XmlFormattersFactory(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "xml";
	}

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("xml");
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, KEY_PARAMETERS);
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}
		LOGGER.info("Processing: {}", engine);

		ILintFixer processor;
		switch (engine) {
		case "jackson":
			JacksonXmlFormatterProperties processorConfig =
					getObjectMapper().convertValue(parameters, JacksonXmlFormatterProperties.class);
			processor = new JacksonXmlFormatter(languageProperties.getSourceCode(), processorConfig);

			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		return processor;
	}

	@Override
	public LanguageProperties makeDefaultProperties() {
		LanguageProperties languageProperties = new LanguageProperties();

		languageProperties.setLanguage(getLanguage());

		List<Map<String, ?>> processors = new ArrayList<>();

		{
			JacksonXmlFormatterProperties engineParameters = new JacksonXmlFormatterProperties();
			JacksonXmlFormatter engine =
					new JacksonXmlFormatter(languageProperties.getSourceCode(), engineParameters);

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, engine.getId())
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}

		languageProperties.setProcessors(processors);

		return languageProperties;
	}
}
