package eu.solven.cleanthat.language.json;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.ALanguageFormatter;
import eu.solven.cleanthat.formatter.ISourceCodeFormatter;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.IStringFormatter;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatter;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatterProperties;

/**
 * Formatter for Json
 *
 * @author Benoit Lacelle
 */
public class JsonFormatter extends ALanguageFormatter implements IStringFormatter {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonFormatter.class);

	public JsonFormatter(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	public String getLanguage() {
		return "json";
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	protected ISourceCodeFormatter makeFormatter(Map<String, ?> rawProcessor, ILanguageProperties languageProperties) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, "engine");
		Map<String, Object> parameters = PepperMapHelper.getAs(rawProcessor, "parameters");
		if (parameters == null) {
			// Some engine takes no parameter
			parameters = Map.of();
		}
		LOGGER.info("Processing: {}", engine);

		ISourceCodeFormatter processor;
		switch (engine) {
		case "jackson":
			JacksonJsonFormatterProperties processorConfig =
					getObjectMapper().convertValue(parameters, JacksonJsonFormatterProperties.class);
			processor = new JacksonJsonFormatter(languageProperties.getSourceCodeProperties(), processorConfig);

			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		return processor;
	}
}
