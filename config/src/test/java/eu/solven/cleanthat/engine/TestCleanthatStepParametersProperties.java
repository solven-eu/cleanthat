package eu.solven.cleanthat.engine;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatStepParametersProperties;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TestCleanthatStepParametersProperties {
	final ObjectMapper objectMapper = ConfigHelpers.makeJsonObjectMapper();

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatStepParametersProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		CleanthatStepParametersProperties p = new CleanthatStepParametersProperties();

		p.add("k1", "v1");

		String json = objectMapper.writeValueAsString(p);

		CleanthatStepParametersProperties backToObject =
				objectMapper.readValue(json, CleanthatStepParametersProperties.class);

		Assert.assertEquals(p, backToObject);
		Assertions.assertThat(backToObject.getCustomProperty("k1")).isEqualTo("v1");
	}

	@Test
	public void testCustomConfig() throws JsonProcessingException {
		CleanthatCustomStepParametersProperties p =
				CleanthatCustomStepParametersProperties.builder().someKey("someValue").build();

		CleanthatStepParametersProperties genericConfig =
				objectMapper.convertValue(p, CleanthatStepParametersProperties.class);

		String json = objectMapper.writeValueAsString(genericConfig);

		CleanthatStepParametersProperties backToGeneric =
				objectMapper.readValue(json, CleanthatStepParametersProperties.class);

		CleanthatCustomStepParametersProperties backToCustomThroughGeneric =
				objectMapper.convertValue(backToGeneric, CleanthatCustomStepParametersProperties.class);

		CleanthatCustomStepParametersProperties backToCustom =
				objectMapper.readValue(json, CleanthatCustomStepParametersProperties.class);

		Assert.assertEquals(p, backToCustomThroughGeneric);
		Assert.assertEquals(p, backToCustom);

	}
}
