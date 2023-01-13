package eu.solven.cleanthat.language;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.pojo.CleanthatMetaProperties;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class TestCleanthatMetaProperties {
	final ObjectMapper objectMapper = ConfigHelpers.makeJsonObjectMapper();

	@Test
	public void testHashcodeEquals() {
		EqualsVerifier.forClass(CleanthatMetaProperties.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		CleanthatMetaProperties p = new CleanthatMetaProperties();

		String json = objectMapper.writeValueAsString(p);

		CleanthatMetaProperties backToObject = objectMapper.readValue(json, CleanthatMetaProperties.class);

		Assert.assertEquals(p, backToObject);
		Assertions.assertThat(backToObject.getLabels()).containsExactly("cleanthat");
		Assertions.assertThat(backToObject.getRefs()).isNotNull();
	}
}
