package eu.solven.cleanthat.language.java.spotless;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.spotless.SpotlessStepProperties;

public class TestSpotlessStepProperties {
	@Test
	public void testJackson() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();

		SpotlessStepProperties p = new SpotlessStepProperties("someStepName");

		p.add("someCustomeKey", "someCustomValue");

		String asString = om.writeValueAsString(p);
		Assertions.assertThat(asString)
				.containsSubsequence("name", "someStepName", "someCustomeKey", "someCustomValue");

		SpotlessStepProperties backAsObject = om.readValue(asString, SpotlessStepProperties.class);

		Assertions.assertThat(backAsObject).isEqualTo(p);

	}
}
