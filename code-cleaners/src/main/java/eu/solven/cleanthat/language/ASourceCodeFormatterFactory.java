package eu.solven.cleanthat.language;

import java.util.Map;
import java.util.Optional;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Abstract class for language formatters
 *
 * @author Benoit Lacelle
 */
public abstract class ASourceCodeFormatterFactory implements ILanguageLintFixerFactory {
	final ConfigHelpers configHelpers;

	public ASourceCodeFormatterFactory(ConfigHelpers configHelpers) {
		this.configHelpers = configHelpers;
	}

	public <T> T convertValue(Object input, Class<T> clazz) {
		return configHelpers.getObjectMapper().convertValue(input, clazz);
	}

	public ConfigHelpers getConfigHelpers() {
		return configHelpers;
	}

	protected Map<String, ?> getParameters(Map<String, ?> rawProcessor) {
		Optional<?> optRawParameters = PepperMapHelper.<Map<String, Object>>getOptionalAs(rawProcessor, KEY_PARAMETERS);

		if (optRawParameters.isPresent()) {
			if (optRawParameters.get() instanceof Map<?, ?>) {
				return (Map<String, ?>) optRawParameters.get();
			} else {
				// We received a real instance of the parameters
				return convertValue(optRawParameters.get(), Map.class);
			}
		} else {
			// Various engines are parameter-less
			return Map.of();
		}
	}
}