package eu.solven.cleanthat.github;

import org.kohsuke.github.GitHub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;

/**
 * The {@link Configuration} enabling {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
public class GithubSpringConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env, ObjectMapper objectMapper) {
		return new GithubWebhookHandlerFactory(env, objectMapper);
	}

	@Bean
	public ICodeProviderFormatter codeProviderFormatter(ObjectMapper objectMapper, IStringFormatter formatter) {
		return new CodeProviderFormatter(objectMapper, formatter);
	}

	@Bean
	public GithubPullRequestCleaner githubPullRequestCleaner(ObjectMapper objectMapper,
			ICodeProviderFormatter formatterProvider) {
		return new GithubPullRequestCleaner(objectMapper, formatterProvider);
	}
}
