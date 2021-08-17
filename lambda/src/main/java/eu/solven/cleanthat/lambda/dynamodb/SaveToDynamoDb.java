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

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

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

	public static void saveToDynamoDb(String table, IWebhookEvent input, AmazonDynamoDB client) {
		String primaryKey = "random-" + UUID.randomUUID();
		LOGGER.info("Save something into DynamoDB table={} primaryKey={}", table, primaryKey);

		DynamoDB dynamodb = new DynamoDB(client);
		Table myTable = dynamodb.getTable(table);
		// https://stackoverflow.com/questions/31813868/aws-dynamodb-on-android-inserting-json-directly

		Map<String, Object> inputAsMap = new LinkedHashMap<>();
		inputAsMap.put("X-GitHub-Delivery", primaryKey);

		inputAsMap.put("datetime", OffsetDateTime.now().toString());

		// TODO We should convert to pure Map, as DynamoDb does not accept custom POJO here
		// see com.amazonaws.services.dynamodbv2.document.internal.ItemValueConformer.transform(Object)
		inputAsMap.put("body", input.getBody());
		inputAsMap.put("headers", input.getHeaders());

		PutItemOutcome outcome = myTable.putItem(Item.fromMap(Collections.unmodifiableMap(inputAsMap)));
		LOGGER.info("PUT metdata for table={} primaryKey={}: {}",
				table,
				primaryKey,
				outcome.getPutItemResult().getSdkHttpMetadata());

	}
}
