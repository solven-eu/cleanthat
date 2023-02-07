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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.code_provider.github.event.CompositeCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.dynamodb.SaveToDynamoDb;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Used to check if given webhook is associated to a valid configuration (e.g. to filter irrelevant repositories).
 * 
 * @author Benoit Lacelle
 *
 */
public class CheckConfigWebhooksLambdaFunction extends AWebhooksLambdaFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckConfigWebhooksLambdaFunction.class);

	public AmazonDynamoDB makeDynamoDbClient() {
		return SaveToDynamoDb.makeDynamoDbClient();
	}

	@Override
	protected Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input) {
		IGitWebhookHandler makeWithFreshJwt = extracted(getAppContext());

		ICodeCleanerFactory cleanerFactory = getAppContext().getBean(CompositeCodeCleanerFactory.class);

		WebhookRelevancyResult processAnswer =
				makeWithFreshJwt.filterWebhookEventTargetRelevantBranch(cleanerFactory, input);

		if (processAnswer.optHeadToClean().isPresent()) {
			AmazonDynamoDB client = makeDynamoDbClient();

			Map<String, Object> acceptedEvent = new LinkedHashMap<>(input.getBody());

			GitRepoBranchSha1 headToClean = processAnswer.optHeadToClean().get();

			ObjectMapper objectMapper = getAppContext().getBean(ObjectMapper.class);
			acceptedEvent.put("refToClean", objectMapper.convertValue(headToClean, Map.class));

			SaveToDynamoDb.saveToDynamoDb("cleanthat_accepted_events",
					new CleanThatWebhookEvent(input.getHeaders(), acceptedEvent),
					client);
		} else {
			LOGGER.info("Rejected due to: {}", processAnswer.optRejectedReason().get());
		}

		return Map.of("whatever", "done");
	}

	public static IGitWebhookHandler extracted(ApplicationContext appContext) {
		IGitWebhookHandlerFactory githubFactory = appContext.getBean(IGitWebhookHandlerFactory.class);

		// TODO Cache the Github instance for the JWT duration
		IGitWebhookHandler makeWithFreshJwt;
		try {
			makeWithFreshJwt = githubFactory.makeWithFreshAuth();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return makeWithFreshJwt;
	}

}
