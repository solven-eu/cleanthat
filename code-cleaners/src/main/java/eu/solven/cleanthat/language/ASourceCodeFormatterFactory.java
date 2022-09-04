package eu.solven.cleanthat.language;

import java.util.Map;
import java.util.Optional;

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

	protected Map<String, ?> getParameters(Map<String, ?> rawProcessor) {
		Optional<?> optRawParameters = PepperMapHelper.<Map<String, Object>>getOptionalAs(rawProcessor, KEY_PARAMETERS);

		if (optRawParameters.isPresent()) {
			if (optRawParameters.get() instanceof Map<?, ?>) {
				return (Map<String, ?>) optRawParameters.get();
			} else {
				// We received a real instance of the parameters
				return getObjectMapper().convertValue(optRawParameters.get(), Map.class);
			}
		} else {
			// Various engines are parameter-less
			return Map.of();
		}
	}
}