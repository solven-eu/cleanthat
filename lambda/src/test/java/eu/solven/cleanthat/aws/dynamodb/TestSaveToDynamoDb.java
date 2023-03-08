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
package eu.solven.cleanthat.aws.dynamodb;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;

import eu.solven.cleanthat.lambda.dynamodb.SaveToDynamoDb;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

public class TestSaveToDynamoDb {
	final IWebhookEvent someInput = Mockito.mock(IWebhookEvent.class);
	final AmazonDynamoDB someClient = Mockito.mock(AmazonDynamoDB.class);

	@Test
	public void testPrimaryKey() {
		Mockito.when(someClient.putItem(Mockito.any(PutItemRequest.class)))
				.thenReturn(Mockito.mock(PutItemResult.class, Mockito.RETURNS_DEEP_STUBS));

		String primaryKey = SaveToDynamoDb.saveToDynamoDb("someTable", someInput, someClient);

		Assertions.assertThat(primaryKey).isNotBlank();
	}

	@Test
	public void testPrimaryKey_alreadyAHeader() {
		Mockito.when(someClient.putItem(Mockito.any(PutItemRequest.class)))
				.thenReturn(Mockito.mock(PutItemResult.class, Mockito.RETURNS_DEEP_STUBS));

		String primaryKey = SaveToDynamoDb.saveToDynamoDb("someTable", someInput, someClient);

		Assertions.assertThat(primaryKey).isNotBlank();
	}
}
