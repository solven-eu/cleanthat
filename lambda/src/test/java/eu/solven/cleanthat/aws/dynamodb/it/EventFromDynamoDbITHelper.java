/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.aws.dynamodb.it;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.dynamodb.SaveToDynamoDb;
import java.util.Map;

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
