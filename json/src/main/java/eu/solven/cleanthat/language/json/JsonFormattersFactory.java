package eu.solven.cleanthat.language.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.language.ASourceCodeFormatterFactory;
import eu.solven.cleanthat.language.ILanguageProperties;
import eu.solven.cleanthat.language.LanguageProperties;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatter;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatterProperties;
import eu.solven.pepper.collection.PepperMapHelper;

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

	@Override
	public Set<String> getFileExtentions() {
		return Set.of("json");
	}

	@SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
	@Override
	public ILintFixer makeLintFixer(Map<String, ?> rawProcessor,
			ILanguageProperties languageProperties,
			ICodeProvider codeProvider) {
		String engine = PepperMapHelper.getRequiredString(rawProcessor, KEY_ENGINE);
		Map<String, ?> parameters = getParameters(rawProcessor);
		LOGGER.info("Processing: {}", engine);

		ILintFixerWithId processor;
		switch (engine) {
		case "jackson":
			JacksonJsonFormatterProperties processorConfig =
					getObjectMapper().convertValue(parameters, JacksonJsonFormatterProperties.class);
			processor = new JacksonJsonFormatter(languageProperties.getSourceCode(), processorConfig);

			break;

		default:
			throw new IllegalArgumentException("Unknown engine: " + engine);
		}

		if (!processor.getId().equals(engine)) {
			throw new IllegalStateException("Inconsistency: " + processor.getId() + " vs " + engine);
		}

		return processor;
	}

	@Override
	public LanguageProperties makeDefaultProperties() {
		LanguageProperties languageProperties = new LanguageProperties();

		languageProperties.setLanguage(getLanguage());

		List<Map<String, ?>> processors = new ArrayList<>();

		{
			JacksonJsonFormatterProperties engineParameters = new JacksonJsonFormatterProperties();
			JacksonJsonFormatter engine =
					new JacksonJsonFormatter(languageProperties.getSourceCode(), engineParameters);

			processors.add(ImmutableMap.<String, Object>builder()
					.put(KEY_ENGINE, engine.getId())
					.put(KEY_PARAMETERS, engineParameters)
					.build());
		}

		languageProperties.setProcessors(processors);

		return languageProperties;
	}
}
