package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

public class TestConfigHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestConfigHelpers.class);

	private static final String EOL = System.lineSeparator();

	@Test
	public void testFromJsonToYaml() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper jsonObjectMapper = ConfigHelpers.makeJsonObjectMapper();
		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(jsonObjectMapper));

		// 'default_as_json' case is not satisfying as we have null in its yaml version
		Stream.of("simple_as_json", "default_as_json").forEach(name -> {
			try {
				CleanthatRepositoryProperties config =
						configHelpers.loadRepoConfig(new ClassPathResource("/config/" + name + ".json"));
				ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
				String asYaml = yamlObjectMapper.writeValueAsString(config);

				LOGGER.debug("Config as yaml: {}{}{}{}{}{}", EOL, "------", EOL, asYaml, EOL, "------");

				String expectedYaml = StreamUtils.copyToString(
						new ClassPathResource("/config/" + name + ".to_yaml.yaml").getInputStream(),
						StandardCharsets.UTF_8);
				Assert.assertEquals("Issue with " + name, expectedYaml, asYaml);
			} catch (IOException e) {
				throw new UncheckedIOException("Issue with: " + name, e);
			}
		});
	}
}
