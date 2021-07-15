package eu.solven.cleanthat.github;

import java.util.List;

import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.event.CodeCleanerFactory;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.ICodeCleanerFactory;

/**
 * The {@link Configuration} enabling {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
public class GithubSpringConfig {

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

	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env, List<ObjectMapper> objectMappers) {
		return new GithubWebhookHandlerFactory(env, objectMappers);
	}

	@Bean
	public ICodeProviderFormatter codeProviderFormatter(List<ObjectMapper> objectMappers, IStringFormatter formatter) {
		return new CodeProviderFormatter(objectMappers, formatter);
	}

	@Bean
	public ICodeCleanerFactory codeCleanerFactory(List<ObjectMapper> objectMappers,
			ICodeProviderFormatter formatterProvider) {
		return new CodeCleanerFactory(objectMappers, formatterProvider);
	}
}
