package eu.solven.cleanthat.aws.dynamodb.it;

import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.dynamodb.SaveToDynamoDb;

public class EventFromDynamoDbITHelper {

	public static Map<String, ?> loadEvent(String table, String key) {
		AmazonDynamoDB dynamoDbClient = SaveToDynamoDb.makeDynamoDbClient();

		GetItemResult item = dynamoDbClient.getItem(new GetItemRequest().withTableName(table)
				.withKey(Map.of(GithubWebhookEvent.X_GIT_HUB_DELIVERY, new AttributeValue().withS(key))));

		Map<String, AttributeValue> dynamoDbItem = item.getItem();
		if (dynamoDbItem == null) {
			throw new IllegalArgumentException("There is no item with key=" + key);
		}

		@SuppressWarnings("deprecation")
		Map<String, ?> dynamoDbPureJson = InternalUtils.toSimpleMapValue(dynamoDbItem);
		return dynamoDbPureJson;
	}
}
