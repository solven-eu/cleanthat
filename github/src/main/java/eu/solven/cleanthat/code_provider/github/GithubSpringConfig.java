package eu.solven.cleanthat.code_provider.github;

import java.util.List;

import org.kohsuke.github.GitHub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.GithubCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.engine.ILanguageLintFixerFactory;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * The {@link Configuration} enabling {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ CodeCleanerSpringConfig.class })
public class GithubSpringConfig {
	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env, List<ObjectMapper> objectMappers) {
		return new GithubWebhookHandlerFactory(env, objectMappers);
	}

	@Bean
	public GithubCodeCleanerFactory githubCodeCleanerFactory(List<ObjectMapper> objectMappers,
			List<ILanguageLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider) {
		return new GithubCodeCleanerFactory(objectMappers, factories, formatterProvider);
	}
}
