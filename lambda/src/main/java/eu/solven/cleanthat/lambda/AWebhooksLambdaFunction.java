package eu.solven.cleanthat.lambda;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.jackson.CustomSnakeCase;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * The main used by AWS Lambda. This is a {@link SpringBootApplication} which is quite fat. There is lighter
 * alternative; but as this will run async, we are fine.
 *
 * @author Benoit Lacelle
 */
// https://github.com/spring-cloud/spring-cloud-function
// https://cloud.spring.io/spring-cloud-static/spring-cloud-function/2.1.1.RELEASE/spring-cloud-function.html
// https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups/log-group/$252Faws$252Flambda$252FupperCase
public abstract class AWebhooksLambdaFunction extends ACleanThatXxxFunction {

	private static final String KEY_BODY = "body";
	private static final Logger LOGGER = LoggerFactory.getLogger(AWebhooksLambdaFunction.class);

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Bean
	public Function<Map<String, ?>, Map<String, ?>> ingressRawWebhook() {
		ObjectMapper objectMapper = appContext.getBean(ObjectMapper.class);

		// DynamoDB prints json as 'S' for AttributeValue.getS(), while default jackson would name this field 's'
		ObjectMapper dynamoDbObjectMapper = objectMapper.copy();
		dynamoDbObjectMapper.setPropertyNamingStrategy(new CustomSnakeCase());

		// https://aws.amazon.com/fr/premiumsupport/knowledge-center/custom-headers-api-gateway-lambda/
		// We would benefit from seeing the headers from Github:
		// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#webhook-payload-object-common-properties
		// X-GitHub-Delivery: 77cc35f0-aea9-11eb-8dcd-63becf50aa7e
		// X-GitHub-Event: pull_request
		// X-GitHub-Hook-ID: 212898303
		// X-GitHub-Hook-Installation-Target-ID: 65550
		// X-GitHub-Hook-Installation-Target-Type: integration
		return input -> {
			Map<String, ?> functionOutput;
			if (input.containsKey("Records")) {
				// This comes from SQS, which pushes SQSEvent
				Collection<Map<String, ?>> records = PepperMapHelper.getRequiredAs(input, "Records");
				LOGGER.info("About to process a batch of {} events from SQS", records.size());
				// https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-sqs/src/main/java/com/amazonaws/services/sqs/model/Message.java
				List<?> output = records.stream().map(r -> {
					Map<String, ?> asMap;
					if (r.containsKey(KEY_BODY)) {
						// SQS
						String body = PepperMapHelper.getRequiredString(r, KEY_BODY);
						Optional<Object> messageAttributes = PepperMapHelper.getOptionalAs(r, "messageAttributes");
						if (messageAttributes.isPresent()) {
							LOGGER.info("Attributes: {}", messageAttributes);
						}

						// SQS transfer the body 'as is'
						try {
							asMap = (Map<String, ?>) objectMapper.readValue(body, Map.class);
						} catch (JsonProcessingException e) {
							LOGGER.warn("Issue while parsing: {}", body);
							LOGGER.warn("Issue while parsing body", e);
							asMap = Collections.<String, Object>emptyMap();
						}

					} else {
						// DynamoDB
						// https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_streams_StreamRecord.html
						// https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Streams.Lambda.Tutorial.html
						// see StreamRecord
						LOGGER.warn("TODO Learn how to process me: {}", r);

						String eventName = PepperMapHelper.getRequiredString(r, "eventName");

						if (!"INSERT".equals(eventName) && !"MODIFY".equals(eventName)) {
							// https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_streams_Record.html
							// We are in a REMOVE event
							LOGGER.info("We discard eventName={}", eventName);
							asMap = Collections.emptyMap();
						} else {
							Map<String, ?> dynamoDbMap = PepperMapHelper.getRequiredMap(r, "dynamodb", "NewImage");

							// We receive from DynamoDb a json in a special format
							// https://stackoverflow.com/questions/32712675/formatting-dynamodb-data-to-normal-json-in-aws-lambda
							// https://stackoverflow.com/questions/37655755/how-to-get-the-pure-json-string-from-dynamodb-stream-new-image
							Map<String, AttributeValue> dynamoDbAttributes = dynamoDbObjectMapper
									.convertValue(dynamoDbMap, new TypeReference<Map<String, AttributeValue>>() {

									});

							asMap = InternalUtils.toSimpleMapValue(dynamoDbAttributes);
						}
					}

					return asMap;
				}).filter(m -> !m.isEmpty()).map(r -> {
					try {
						return processOneEvent(new GithubWebhookEvent(r));
					} catch (RuntimeException e) {
						LOGGER.warn("Issue with one message of a batch of " + records.size() + " messages", e);
						return Collections.singletonMap("ARG", e.getMessage());
					}
				}).collect(Collectors.toList());
				functionOutput = Map.of("sqs", output);
			} else {
				IWebhookEvent event;
				if (input.containsKey(KEY_BODY) && input.containsKey("headers")) {
					// see CheckWebhooksLambdaFunction.saveToDynamoDb(String, IWebhookEvent, AmazonDynamoDB)
					// event = SaveToDynamoDb.NONE;
					event = new CleanThatWebhookEvent((Map<String, ?>) input.get("headers"),
							(Map<String, ?>) input.get(KEY_BODY));
				} else {
					event = new GithubWebhookEvent(input);
				}
				functionOutput = processOneEvent(event);
			}
			LOGGER.info("Output: {}", functionOutput);
			return functionOutput;
		};
	}
}
