package eu.solven.cleanthat.config.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;

/**
 * Set of beans related to reading/writing CleanThat configuration
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
public class ConfigSpringConfig {

	// Primary as most situations to write something is to write JSON
	@Bean
	@Primary
	@Qualifier("json")
	public ObjectMapper jsonObjectMapper() {
		return ConfigHelpers.makeJsonObjectMapper();
	}

	// YAML is still very useful to read configuration
	@Bean
	@Qualifier("yaml")
	public ObjectMapper yamlObjectMapper() {
		return ConfigHelpers.makeYamlObjectMapper();
	}
}
