package eu.solven.cleanthat.engine;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Some specialization of ICleanthatStepParametersProperties
 * 
 * @author Benoit Lacelle
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
@Builder
@Jacksonized
public class CleanthatCustomStepParametersProperties implements ICleanthatStepParametersProperties {

	@Builder.Default
	private String someKey = "someDefaultValue";

	@Override
	public Object getCustomProperty(String key) {
		if ("some_key".equalsIgnoreCase(key)) {
			return someKey;
		}
		return null;
	}
}
