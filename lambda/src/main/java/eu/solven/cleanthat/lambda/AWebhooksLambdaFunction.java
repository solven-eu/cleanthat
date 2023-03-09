/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.lambda;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.seratch.jslack.Slack;

import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.jackson.CustomSnakeCase;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.pepper.collection.PepperMapHelper;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(AWebhooksLambdaFunction.class);

	private static final String KEY_BODY = "body";
	private static final String KEY_HEADERS = "headers";

	// Used to notify specific events
	@Bean
	public Slack slack() {
		return new Slack();
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Bean
	public Function<Map<String, ?>, Map<String, ?>> ingressRawWebhook() {
		ObjectMapper objectMapper = appContext.getBean(ObjectMapper.class);

		var dynamoDbObjectMapper = configureForDynamoDb(objectMapper);

		// https://aws.amazon.com/fr/premiumsupport/knowledge-center/custom-headers-api-gateway-lambda/
		// We would benefit from seeing the headers from Github:
		// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#webhook-payload-object-common-properties
		// X-GitHub-Delivery: 77cc35f0-aea9-11eb-8dcd-63becf50aa7e
		// X-GitHub-Event: pull_request
		// X-GitHub-Hook-ID: 212898303
		// X-GitHub-Hook-Installation-Target-ID: 65550
		// X-GitHub-Hook-Installation-Target-Type: integration
		// TODO SHould this implement RoutingFunction ? (e.g. to get AWS Lambda Context)
		return input -> {
			Map<String, ?> functionOutput;

			if (input.containsKey("Records")) {
				// This comes from SQS, which pushes SQSEvent
				Collection<Map<String, ?>> records = PepperMapHelper.getRequiredAs(input, "Records");
				LOGGER.info("About to process a batch of {} events from AWS", records.size());
				// https://github.com/aws/aws-sdk-java/blob/master/aws-java-sdk-sqs/src/main/java/com/amazonaws/services/sqs/model/Message.java
				List<?> output = records.stream().map(r -> {
					Map<String, ?> asMap;
					try {
						if (r.containsKey(KEY_BODY)) {
							asMap = parseSqsEvent(objectMapper, r);
						} else {
							asMap = parseDynamoDbEvent(dynamoDbObjectMapper, r);
						}
					} catch (RuntimeException e) {
						logEvent(objectMapper, input);
						throw new RuntimeException("Issue parsing AWS message", e);
					}

					return asMap;
				}).filter(m -> !m.isEmpty()).map(r -> {
					logEvent(objectMapper, r);

					var event = wrapAsEvent(r);
					try {
						return processOneEvent(event);
					} catch (RuntimeException e) {
						LOGGER.warn("Issue with one message of a batch of " + records.size() + " messages", e);
						return Collections.singletonMap("ARG", e.getMessage());
					}
				}).collect(Collectors.toList());
				functionOutput = Map.of("sqs", output);
			} else {
				// This would happen on Lambda direct invocation
				// But we always try to rely on events(SQS, DynamoDB, ...)
				// It may also happens in localhost invocation (e.g. ITProcessLocallyDynamoDbEvent_ExecuteClean)
				logEvent(objectMapper, input);

				var event = wrapAsEvent(input);
				functionOutput = processOneEvent(event);
			}
			LOGGER.info("Output: {}", functionOutput);
			return functionOutput;
		};

	}

	private void logEvent(ObjectMapper objectMapper, Map<String, ?> input) {
		try {
			LOGGER.info("TODO Add unit-test for: {}", objectMapper.writeValueAsString(input));
		} catch (JsonProcessingException ee) {
			LOGGER.warn("Issue printing JSON", ee);
		}
	}

	public static ObjectMapper configureForDynamoDb(ObjectMapper objectMapper) {
		// DynamoDB prints json as 'S' for AttributeValue.getS(), while default jackson would name this field 's'
		var dynamoDbObjectMapper = objectMapper.copy();

		// DynamoDB exclude null from AttributeValue fields
		dynamoDbObjectMapper.setSerializationInclusion(Include.NON_NULL);

		dynamoDbObjectMapper.setPropertyNamingStrategy(new CustomSnakeCase());
		return dynamoDbObjectMapper;
	}

	public IWebhookEvent wrapAsEvent(Map<String, ?> input) {

		IWebhookEvent event;
		if (input.containsKey(KEY_BODY) && input.containsKey(KEY_HEADERS)) {
			String eventKey = PepperMapHelper.getRequiredString(input, GithubWebhookEvent.X_GIT_HUB_DELIVERY);
			// see CheckWebhooksLambdaFunction.saveToDynamoDb(String, IWebhookEvent, AmazonDynamoDB)
			// event = SaveToDynamoDb.NONE;

			Map<String, Object> rootBody = PepperMapHelper.getRequiredMap(input, KEY_BODY);

			if (rootBody.containsKey(GithubWebhookEvent.KEY_GITHUB)) {
				Map<String, Object> github = PepperMapHelper.getRequiredMap(rootBody, GithubWebhookEvent.KEY_GITHUB);

				Map<String, Object> githubHeaders = PepperMapHelper.getRequiredMap(github, KEY_HEADERS);

				if (githubHeaders.containsKey(GithubWebhookEvent.X_GIT_HUB_DELIVERY)) {
					Object realDelivery = githubHeaders.put(GithubWebhookEvent.X_GIT_HUB_DELIVERY, eventKey);
					if (realDelivery != null && !realDelivery.equals(eventKey)) {
						LOGGER.warn("We replace a real {}={} by the fake {}",
								GithubWebhookEvent.X_GIT_HUB_DELIVERY,
								realDelivery,
								eventKey);
					}
				} else {
					// Headers is typically empty as we fails fetching headers from API Gateway
					githubHeaders = new LinkedHashMap<>(githubHeaders);

					// TODO We should push this to the headers next to the actual github body, which may be deeper
					// We should also push it when the initial event is received
					githubHeaders.put(GithubWebhookEvent.X_GIT_HUB_DELIVERY, eventKey);

					// Install the updated headers
					github.put(KEY_HEADERS, githubHeaders);
				}
			}

			Map<String, Object> headers = PepperMapHelper.getRequiredMap(input, KEY_HEADERS);

			event = new CleanThatWebhookEvent(headers, rootBody);
		} else {
			// This is a payload right from Github
			// We lack headers as we are not able to forward them from SQS
			event = new GithubWebhookEvent(input);
		}
		return event;
	}

	public Map<String, ?> parseDynamoDbEvent(ObjectMapper dynamoDbObjectMapper, Map<String, ?> r) {
		Map<String, ?> asMap;
		// DynamoDB
		// https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_streams_StreamRecord.html
		// https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Streams.Lambda.Tutorial.html
		// see StreamRecord
		LOGGER.debug("TODO Learn how to process me: {}", r);

		var eventName = PepperMapHelper.getRequiredString(r, "eventName");

		if (
		// INSERT event: this is something new process
		"INSERT".equals(eventName)
				// MODIFY event: this is typically an admin which modify an event manually
				|| "MODIFY".equals(eventName)) {
			Map<String, ?> dynamoDbMap = PepperMapHelper.getRequiredMap(r, "dynamodb", "NewImage");

			// We receive from DynamoDb a json in a special format
			// https://stackoverflow.com/questions/32712675/formatting-dynamodb-data-to-normal-json-in-aws-lambda
			// https://stackoverflow.com/questions/37655755/how-to-get-the-pure-json-string-from-dynamodb-stream-new-image
			Map<String, AttributeValue> dynamoDbAttributes =
					dynamoDbObjectMapper.convertValue(dynamoDbMap, new TypeReference<Map<String, AttributeValue>>() {

					});

			asMap = InternalUtils.toSimpleMapValue(dynamoDbAttributes);

			LOGGER.info("processing {}={} by {}",
					GithubWebhookEvent.X_GIT_HUB_DELIVERY,
					PepperMapHelper.getRequiredString(asMap, GithubWebhookEvent.X_GIT_HUB_DELIVERY),
					this.getClass().getName());
		} else {
			// https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_streams_Record.html
			// We are in a REMOVE event
			LOGGER.info("We discard eventName={}", eventName);
			asMap = Collections.emptyMap();
		}
		return asMap;
	}

	public Map<String, ?> parseSqsEvent(ObjectMapper objectMapper, Map<String, ?> r) {
		Map<String, ?> asMap;
		// SQS
		var body = PepperMapHelper.getRequiredString(r, KEY_BODY);
		Optional<Object> messageAttributes = PepperMapHelper.getOptionalAs(r, "messageAttributes");
		if (messageAttributes.isPresent()) {
			LOGGER.info("Attributes: {}", messageAttributes);
		}

		// SQS transfer the body 'as is'
		try {
			asMap = objectMapper.readValue(body, Map.class);
		} catch (JsonProcessingException e) {
			LOGGER.warn("Issue while parsing: {}", body);
			LOGGER.warn("Issue while parsing body", e);
			asMap = Collections.<String, Object>emptyMap();
		}
		return asMap;
	}
}
