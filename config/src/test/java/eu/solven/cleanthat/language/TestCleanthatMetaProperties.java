package eu.solven.cleanthat.language;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestCleanthatMetaProperties {
	final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		CleanthatMetaProperties p = new CleanthatMetaProperties();

		String json = objectMapper.writeValueAsString(p);

		CleanthatMetaProperties backToObject = objectMapper.readValue(json, CleanthatMetaProperties.class);

		Assert.assertEquals(p, backToObject);
		Assertions.assertThat(backToObject.getLabels()).isEmpty();
		Assertions.assertThat(backToObject.getRefs()).isNotNull();
	}
}
