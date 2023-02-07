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
package eu.solven.cleanthat.lambda.step1_checkconfiguration;

import java.io.IOException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.CompositeCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.HeadAndOptionalBase;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

@RunWith(SpringRunner.class)
@MockBean({ IGitWebhookHandlerFactory.class, CompositeCodeCleanerFactory.class, ObjectMapper.class })
public class TestCheckConfigWebhooksLambdaFunction {

	final IGitWebhookHandler webhookHandler = Mockito.mock(IGitWebhookHandler.class);
	final AmazonDynamoDB dynamoDb = Mockito.mock(AmazonDynamoDB.class);

	@Autowired
	ApplicationContext appContext;

	@Before
	public void prepareMocks() throws BeansException, IOException {
		Mockito.when(appContext.getBean(IGitWebhookHandlerFactory.class).makeWithFreshAuth())
				.thenReturn(webhookHandler);

		PutItemResult result = new PutItemResult();
		Mockito.when(dynamoDb.putItem(Mockito.any(PutItemRequest.class))).thenReturn(result);
		SdkHttpMetadata httpMetadata = Mockito.mock(SdkHttpMetadata.class);
		result.setSdkHttpMetadata(httpMetadata);
	}

	@Test
	public void testPersistInDynamoDb() {
		CheckConfigWebhooksLambdaFunction function = new CheckConfigWebhooksLambdaFunction() {
			@Override
			public AmazonDynamoDB makeDynamoDbClient() {
				return dynamoDb;
			}
		};
		function.setApplicationContext(appContext);

		IWebhookEvent input = Mockito.mock(IWebhookEvent.class);

		GitRepoBranchSha1 head =
				new GitRepoBranchSha1("someUser/someRepoName", "refs/heads/someBranchName", "someSha1");
		Mockito.when(webhookHandler
				.filterWebhookEventTargetRelevantBranch(appContext.getBean(ICodeCleanerFactory.class), input))
				.thenReturn(WebhookRelevancyResult.relevant(new HeadAndOptionalBase(head, Optional.empty())));

		function.unsafeProcessOneEvent(input);

		Mockito.verify(dynamoDb).putItem(Mockito.any(PutItemRequest.class));
	}
}
