package io.cormoran.cleanthat.lambda;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.cormoran.cleanthat.sentry.SentryMvcSpringConfig;

/**
 * The main used by AWS Lambda. This is a {@link SpringBootApplication} which is quite fat. There is lighter
 * alternative; but as this will run async, we are fine.
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/spring-cloud/spring-cloud-function
// https://cloud.spring.io/spring-cloud-static/spring-cloud-function/2.1.1.RELEASE/spring-cloud-function.html
@SpringBootApplication
@Import({ SentryMvcSpringConfig.class })
public class FunctionConfiguration {

	/*
	 * You need this main method or explicit <start-class>example.FunctionConfiguration</start-class> in the POM to
	 * ensure boot plug-in makes the correct entry
	 */
	public static void main(String[] args) {
		SpringApplication.run(FunctionConfiguration.class, args);
	}

	@Bean
	public IGithubWebhookHandler githubWebhookHandler(GitHub github) {
		return new GithubWebhookHandler(github);
	}

	@Bean
	public GitHub github() throws IOException {
		// TODO Get JWT following https://github-api.kohsuke.org/githubappjwtauth.html
		return GitHubBuilder.fromEnvironment().build();
	}

	@Bean
	public Function<Map<String, ?>, Map<String, ?>> uppercase(IGithubWebhookHandler githubWebhookHandler) {
		return input -> githubWebhookHandler.processWebhookBody(input);
	}
}