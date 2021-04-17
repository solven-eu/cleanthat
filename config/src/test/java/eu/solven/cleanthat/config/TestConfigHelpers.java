package eu.solven.cleanthat.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

public class TestConfigHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestConfigHelpers.class);

	@Test
	public void testFromJsonToYaml() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper jsonObjectMapper = new ObjectMapper();
		ConfigHelpers configHelpers = new ConfigHelpers(jsonObjectMapper);

		CleanthatRepositoryProperties config =
				configHelpers.loadRepoConfig(new ClassPathResource("/config/simple_as_json.json"));

		ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
		String asYaml = yamlObjectMapper.writeValueAsString(config);

		LOGGER.debug("Config as yaml: {}{}{}{}{}",
				System.lineSeparator(),
				"------",
				asYaml,
				System.lineSeparator(),
				"------");

		Assert.assertEquals(
				StreamUtils.copyToString(new ClassPathResource("/config/simple_as_json.to_yaml.yaml").getInputStream(),
						StandardCharsets.UTF_8),
				asYaml);
	}
}
