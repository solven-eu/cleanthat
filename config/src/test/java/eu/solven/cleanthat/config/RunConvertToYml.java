package eu.solven.cleanthat.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import eu.solven.cleanthat.github.CleanthatRepositoryProperties;

public class RunConvertToYml {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunConvertToYml.class);

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper jsonObjectMapper = new ObjectMapper();
		ConfigHelpers configHelpers = new ConfigHelpers(jsonObjectMapper);

		CleanthatRepositoryProperties config =
				configHelpers.loadRepoConfig(new FileSystemResource("../cleanthat.json"));

		ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER));
		String asYaml = yamlObjectMapper.writeValueAsString(config);

		LOGGER.info("Config as yaml: {}{}{}{}{}{}",
				System.lineSeparator(),
				"------",
				System.lineSeparator(),
				asYaml,
				System.lineSeparator(),
				"------");
	}
}
