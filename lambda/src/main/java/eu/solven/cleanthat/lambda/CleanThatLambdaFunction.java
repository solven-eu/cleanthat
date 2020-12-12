package eu.solven.cleanthat.lambda;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import io.sentry.Sentry;

/**
 * The main used by AWS Lambda. This is a {@link SpringBootApplication} which is quite fat. There is lighter
 * alternative; but as this will run async, we are fine.
 *
 * @author Benoit Lacelle
 */
// https://github.com/spring-cloud/spring-cloud-function
// https://cloud.spring.io/spring-cloud-static/spring-cloud-function/2.1.1.RELEASE/spring-cloud-function.html
// https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Faws$252Flambda$252FupperCase
@SpringBootApplication
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
	public Function<Map<String, ?>, Map<String, ?>> uppercase(
			// IHub sentryClient,
			ObjectMapper objectMapper,
			GithubWebhookHandlerFactory githubFactory) {
		return input -> {
			if (input.containsKey("Records")) {
				// This comes from SQS, which pushes SQSEvent

				Collection<Map<String, ?>> records = PepperMapHelper.getRequiredAs(input, "Records");

				LOGGER.info("About to process a batch of {} events from SQS", records.size());

				// https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-sqs/src/main/java/com/amazonaws/services/sqs/model/Message.java
				List<?> output = records.stream().map(r -> {
					String body = PepperMapHelper.getRequiredString(r, "body");

					// SQS transfer the body 'as is'
					try {
						return (Map<String, ?>) objectMapper.readValue(body, Map.class);
					} catch (JsonProcessingException e) {
						LOGGER.warn("Issue while parsing: {}", body);
						LOGGER.warn("Issue while parsing body", e);
						return Collections.<String, Object>emptyMap();
					}
				})
						.filter(m -> !m.isEmpty())
						.map(r -> processOneMessage(objectMapper, githubFactory, r))
						.collect(Collectors.toList());
				return Map.of("sqs", output);
			} else {
				return processOneMessage(objectMapper, githubFactory, input);
			}
		};
	}

	private Map<String, ?> processOneMessage(ObjectMapper objectMapper,
			GithubWebhookHandlerFactory githubFactory,
			Map<String, ?> input) {
		try {
			// We log the payload temporarily, in order to have easy access to metadata
			LOGGER.info("TMP payload: {}", objectMapper.writeValueAsString(input));
			// TODO Cache the Github instance for the JWT duration
			JavaFormatter formatter = new JavaFormatter(objectMapper);
			return githubFactory.makeWithFreshJwt()
					.processWebhookBody(input, formatter, new GithubPullRequestCleaner(objectMapper, formatter));
		} catch (IOException | JOSEException | RuntimeException e) {
			Sentry.captureException(e, "Lambda");
			throw new RuntimeException(e);
		}
	}
}
