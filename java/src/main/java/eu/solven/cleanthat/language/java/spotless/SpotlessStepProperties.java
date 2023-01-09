package eu.solven.cleanthat.language.java.spotless;

import java.util.LinkedHashMap;
import java.util.Map;

import com.diffplug.spotless.FormatterStep;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * Helps configuring a {@link FormatterStep}
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@SuppressWarnings("PMD.ImmutableField")
@Data
@JsonIgnoreProperties({ "custom_properties" })
public class SpotlessStepProperties {
	private final String name;

	// https://stackoverflow.com/questions/32235993/mix-of-standard-and-dynamic-properties-in-jackson-mapping
	private Map<String, Object> customProperties = new LinkedHashMap<>();

	@JsonAnySetter
	public void add(String key, String value) {
		customProperties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}

	public Object getCustomProperty(String key) {
		return customProperties.get(key);
	}
}
