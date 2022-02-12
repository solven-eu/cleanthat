package eu.solven.cleanthat.language;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
public abstract class ASourceCodeFormatterFactory implements ILanguageLintFixerFactory {
	final ObjectMapper objectMapper;

	public ASourceCodeFormatterFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	protected Map<String, Object> getParameters(Map<String, ?> rawProcessor) {
		return PepperMapHelper.<Map<String, Object>>getOptionalAs(rawProcessor, KEY_PARAMETERS)
				// Some engines take no parameter
				.orElse(Map.of());
	}
}
