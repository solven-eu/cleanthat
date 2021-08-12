package eu.solven.cleanthat.language;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
public abstract class ASourceCodeFormatterFactory implements ISourceCodeFormatterFactory {
	final ObjectMapper objectMapper;

	public ASourceCodeFormatterFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}
}
