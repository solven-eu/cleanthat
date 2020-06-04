package eu.solven.cleanthat.lambda;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import io.cormoran.cleanthat.formatter.eclipse.JavaFormatter;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatLambdaFunction.class);

	public static void main(String[] args) {
		SpringApplication.run(CleanThatLambdaFunction.class, args);
	}

	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env, ObjectMapper objectMapper) {
		return new GithubWebhookHandlerFactory(env, objectMapper);
	}

	@Bean
	public Function<Map<String, ?>, Map<String, ?>> uppercase(ObjectMapper objectMapper,
			GithubWebhookHandlerFactory githubFactory) {
		return input -> {
			try {
				LOGGER.info("TMP payload: {}", objectMapper.writeValueAsString(input));

				// TODO Cache the Github instance for the JWT duration
				return githubFactory.makeWithFreshJwt().processWebhookBody(input, new JavaFormatter());
			} catch (IOException | JOSEException e) {
				throw new RuntimeException(e);
			}
		};
	}
}