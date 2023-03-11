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
package eu.solven.cleanthat.lambda.dynamodb;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;

import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * Helps saving into DynamoDB
 * 
 * @author Benoit Lacelle
 *
 */
public class SaveToDynamoDb {
	private static final Logger LOGGER = LoggerFactory.getLogger(SaveToDynamoDb.class);

	protected SaveToDynamoDb() {
		// hidden
	}

	public static final IWebhookEvent NONE = new IWebhookEvent() {

		@Override
		public Map<String, ?> getHeaders() {
			throw new IllegalArgumentException();
		}

		@Override
		public Map<String, ?> getBody() {
			throw new IllegalArgumentException();
		}
	};

	public static AmazonDynamoDB makeDynamoDbClient() {
		DefaultAWSCredentialsProviderChain defaultCredentials = DefaultAWSCredentialsProviderChain.getInstance();
		return makeDynamoDbClient(defaultCredentials);
	}

	public static AmazonDynamoDB makeDynamoDbClient(AWSCredentialsProvider defaultCredentials) {
		AmazonDynamoDB client = AmazonDynamoDBClient.builder()
				// The region is meaningless for local DynamoDb but required for client builder validation
				.withRegion(Regions.US_EAST_1)
				.withCredentials(defaultCredentials)
				.build();
		return client;
	}

	public static String saveToDynamoDb(String table, IWebhookEvent input, AmazonDynamoDB client) {
		// We re-use the same xGitHubDelivery for the different steps (checkEvent, checkConfig, executeClean)
		String primaryKey = PepperMapHelper.getOptionalString(input.getHeaders(), GithubWebhookEvent.X_GIT_HUB_DELIVERY)
				.orElseGet(() -> PepperMapHelper
						.getOptionalString(input.getBody(),
								GithubWebhookEvent.KEY_HEADERS,
								GithubWebhookEvent.X_GIT_HUB_DELIVERY)
						.orElseGet(
								() -> PepperMapHelper
										.getOptionalString(input.getBody(),
												GithubWebhookEvent.KEY_GITHUB,
												GithubWebhookEvent.KEY_HEADERS,
												GithubWebhookEvent.X_GIT_HUB_DELIVERY)
										.orElseGet(() -> {
											return randomEventKey(input);
										})

						));

		LOGGER.info("Save something into DynamoDB table={} primaryKey={}", table, primaryKey);

		DynamoDB dynamodb = new DynamoDB(client);
		Table myTable = dynamodb.getTable(table);
		// https://stackoverflow.com/questions/31813868/aws-dynamodb-on-android-inserting-json-directly

		Map<String, Object> inputAsMap = preparePayloadAsMap(input, primaryKey);

		PutItemOutcome outcome = myTable.putItem(Item.fromMap(Collections.unmodifiableMap(inputAsMap)));
		LOGGER.info("PUT metadata for table={} primaryKey={}: {}",
				table,
				primaryKey,
				outcome.getPutItemResult().getSdkHttpMetadata().getHttpStatusCode());

		return primaryKey;

	}

	private static Map<String, Object> preparePayloadAsMap(IWebhookEvent input, String primaryKey) {
		Map<String, Object> inputAsMap = new LinkedHashMap<>();
		inputAsMap.put(GithubWebhookEvent.X_GIT_HUB_DELIVERY, primaryKey);

		inputAsMap.put("datetime", OffsetDateTime.now().toString());

		// TODO We should convert to pure Map, as DynamoDb does not accept custom POJO here
		// see com.amazonaws.services.dynamodbv2.document.internal.ItemValueConformer.transform(Object)
		inputAsMap.put(GithubWebhookEvent.KEY_HEADERS, input.getHeaders());
		inputAsMap.put(GithubWebhookEvent.KEY_BODY, input.getBody());
		return inputAsMap;
	}

	private static String randomEventKey(IWebhookEvent input) {
		var randomEventKey = generateRandomxGithubDelivery();

		// This may happen on step0, as we lack a real xGithubDelivery (due to SQS not
		// transmitting
		// headers)
		// Else, it is a bug where we generate a new xGithubDelivery on each step (fixed
		// around
		// 2023-03)
		LOGGER.warn("We generate a random {}={} for headers={} body={}",
				GithubWebhookEvent.X_GIT_HUB_DELIVERY,
				randomEventKey,
				input.getHeaders(),
				input.getBody());

		return randomEventKey;
	}

	public static String generateRandomxGithubDelivery() {
		return "random-" + UUID.randomUUID();
	}
}
