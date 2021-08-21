package eu.solven.cleanthat.language.json;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatter;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatterProperties;

/**
 * Formatter for Json
 *
 * @author Benoit Lacelle
 */
public class JsonFormattersFactory extends ASourceCodeFormatterFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonFormattersFactory.class);

	public JsonFormattersFactory(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "json";
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, "parameters");
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}
		LOGGER.info("Processing: {}", engine);

		ILintFixer processor;
		switch (engine) {
		case "jackson":
			JacksonJsonFormatterProperties processorConfig =
					getObjectMapper().convertValue(parameters, JacksonJsonFormatterProperties.class);
			processor = new JacksonJsonFormatter(languageProperties.getSourceCode(), processorConfig);

			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		return processor;
	}
}
