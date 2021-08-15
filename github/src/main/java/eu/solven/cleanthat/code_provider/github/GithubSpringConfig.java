package eu.solven.cleanthat.code_provider.github;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.CodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.CodeFormatterApplier;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.language.ICodeFormatterApplier;
import eu.solven.cleanthat.language.ILanguageFormatterFactory;
import eu.solven.cleanthat.language.ISourceCodeFormatterFactory;
import eu.solven.cleanthat.language.StringFormatterFactory;

/**
 * The {@link Configuration} enabling {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
public class GithubSpringConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubSpringConfig.class);

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
	public ICodeFormatterApplier codeFormatterApplier() {
		return new CodeFormatterApplier();
	}

	@Bean
	public ILanguageFormatterFactory stringFormatterFactory(List<ISourceCodeFormatterFactory> stringFormatters) {
		Map<String, ISourceCodeFormatterFactory> asMap = new LinkedHashMap<>();

		stringFormatters.forEach(sf -> {
			String language = sf.getLanguage();
			LOGGER.info("Formatter registered for language={}: {}", language, sf);
			asMap.put(language, sf);
		});

		return new StringFormatterFactory(asMap);
	}

	@Bean
	public ICodeProviderFormatter codeProviderFormatter(List<ObjectMapper> objectMappers,
			ILanguageFormatterFactory formatterFactory,
			ICodeFormatterApplier formatterApplier) {
		return new CodeProviderFormatter(objectMappers, formatterFactory, formatterApplier);
	}

	@Bean
	public ICodeCleanerFactory codeCleanerFactory(List<ObjectMapper> objectMappers,
			ICodeProviderFormatter formatterProvider) {
		return new CodeCleanerFactory(objectMappers, formatterProvider);
	}
}
