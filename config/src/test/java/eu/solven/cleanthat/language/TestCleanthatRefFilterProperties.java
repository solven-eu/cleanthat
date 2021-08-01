package eu.solven.cleanthat.language;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.github.CleanthatRefFilterProperties;

public class TestCleanthatRefFilterProperties {
	final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testDefaultConstructor() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		String json = objectMapper.writeValueAsString(p);

		CleanthatRefFilterProperties backToObject = objectMapper.readValue(json, CleanthatRefFilterProperties.class);

		Assert.assertEquals(p, backToObject);
	}

	@Test
	public void testEmptyJson() throws JsonProcessingException {
		CleanthatRefFilterProperties p = new CleanthatRefFilterProperties();

		String json = "{}";

		CleanthatRefFilterProperties backToObject = objectMapper.readValue(json, CleanthatRefFilterProperties.class);

		Assert.assertEquals(p, backToObject);
	}
}
