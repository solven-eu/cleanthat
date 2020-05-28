package eu.solven.cleanthat.lambda;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.GithubWebhookHandlerFactory;
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
public class CleanThatLambdaFunction {

	/*
	 * You need this main method or explicit <start-class>example.FunctionConfiguration</start-class> in the POM to
	 * ensure boot plug-in makes the correct entry
	 */
	public static void main(String[] args) {
		SpringApplication.run(CleanThatLambdaFunction.class, args);
	}

	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env) {
		return new GithubWebhookHandlerFactory(env);
	}

	@Bean
	public Function<Map<String, ?>, Map<String, ?>> uppercase(GithubWebhookHandlerFactory githubFactory) {
		return input -> {
			try {
				// TODO Cache the Github instance for the JWT duration
				return githubFactory.makeWithFreshJwt().processWebhookBody(input);
			} catch (IOException | JOSEException e) {
				throw new RuntimeException(e);
			}
		};
	}
}