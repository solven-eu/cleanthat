package eu.solven.cleanthat.config;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;

public class RunConvertToYml {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunConvertToYml.class);

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper jsonObjectMapper = ConfigHelpers.makeJsonObjectMapper();
		ConfigHelpers configHelpers = new ConfigHelpers(Arrays.asList(jsonObjectMapper));

		CleanthatRepositoryProperties config =
				configHelpers.loadRepoConfig(new FileSystemResource("../cleanthat.json"));

		ObjectMapper yamlObjectMapper = ConfigHelpers.makeYamlObjectMapper();
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
