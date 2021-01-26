package eu.solven.cleanthat.lambda;

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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;

/**
 * The main used by AWS Lambda. This is a {@link SpringBootApplication} which is quite fat. There is lighter
 * alternative; but as this will run async, we are fine.
 *
 * @author Benoit Lacelle
 */
// https://github.com/spring-cloud/spring-cloud-function
// https://cloud.spring.io/spring-cloud-static/spring-cloud-function/2.1.1.RELEASE/spring-cloud-function.html
// https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Faws$252Flambda$252FupperCase
public class CleanThatLambdaFunction extends ACleanThatXxxFunction {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatLambdaFunction.class);

	public static void main(String[] args) {
		SpringApplication.run(CleanThatLambdaFunction.class, args);
	}

	@Bean
	public Function<Map<String, ?>, Map<String, ?>> uppercase(ApplicationContext appContext) {
		ObjectMapper objectMapper = appContext.getBean(ObjectMapper.class);

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
				}).filter(m -> !m.isEmpty()).map(r -> processOneMessage(appContext, r)).collect(Collectors.toList());
				return Map.of("sqs", output);
			} else {
				return processOneMessage(appContext, input);
			}
		};
	}
}
