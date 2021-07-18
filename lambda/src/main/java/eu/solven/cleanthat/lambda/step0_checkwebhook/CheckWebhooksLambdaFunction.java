package eu.solven.cleanthat.lambda.step0_checkwebhook;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.github.event.pojo.GithubWebhookRelevancyResult;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;

/**
 * Used to filter relevant webhooks for useless webhooks.
 * 
 * This first step should not depends at all on the CodeProvider API (i.e. it works without having to authenticate
 * ourselves at all). We just analyse the webhook content to filter out what's irrelevant.
 * 
 * @author Benoit Lacelle
 *
 */
public class CheckWebhooksLambdaFunction extends AWebhooksLambdaFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckWebhooksLambdaFunction.class);

	public static void main(String[] args) {
		SpringApplication.run(CheckWebhooksLambdaFunction.class, args);
	}

	@Override
	protected Map<String, ?> unsafeProcessOneEvent(ApplicationContext appContext, IWebhookEvent input) {
		GithubWebhookHandlerFactory githubFactory = appContext.getBean(GithubWebhookHandlerFactory.class);

		GithubWebhookEvent githubEvent = (GithubWebhookEvent) input;

		// TODO Cache the Github instance for the JWT duration
		IGithubWebhookHandler makeWithFreshJwt;
		try {
			makeWithFreshJwt = githubFactory.makeWithFreshJwt();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

		GithubWebhookRelevancyResult processAnswer = makeWithFreshJwt.filterWebhookEventRelevant(githubEvent);

		if (processAnswer.isPrOpen() || processAnswer.isPushBranch()) {
			AmazonDynamoDB client = makeDynamoDbClient();

			Map<String, Object> acceptedEvent = new LinkedHashMap<>();

			// We may add details from processAnswer
			acceptedEvent.put("github", Map.of("body", githubEvent.getBody(), "headers", githubEvent.getHeaders()));

			saveToDynamoDb("cleanthat_webhooks_github", new CleanThatWebhookEvent(Map.of(), acceptedEvent), client);
		}

		return Map.of("whatever", "done");
	}

	public static AmazonDynamoDB makeDynamoDbClient() {
		AmazonDynamoDB client = AmazonDynamoDBClient.builder()
				// The region is meaningless for local DynamoDb but required for client builder validation
				.withRegion(Regions.US_EAST_2)
				// .credentialsProvider( new DefaultAWSCredentialsProviderChain())
				.build();
		return client;
	}

	public static void saveToDynamoDb(String table, IWebhookEvent input, AmazonDynamoDB client) {
		LOGGER.info("Save something into DynamoDB");

		DynamoDB dynamodb = new DynamoDB(client);
		Table myTable = dynamodb.getTable(table);
		// https://stackoverflow.com/questions/31813868/aws-dynamodb-on-android-inserting-json-directly

		Map<String, Object> inputAsMap = new LinkedHashMap<>();
		inputAsMap.put("body", input.getBody());
		inputAsMap.put("headers", input.getHeaders());

		myTable.putItem(Item.fromMap(Collections.unmodifiableMap(inputAsMap)));
	}
}
