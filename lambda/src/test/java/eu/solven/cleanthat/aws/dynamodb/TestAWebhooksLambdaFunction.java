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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.aws.dynamodb.TestAWebhooksLambdaFunction.ForTestsWebhooksLambdaFunction;
import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ForTestsWebhooksLambdaFunction.class })
public class TestAWebhooksLambdaFunction {
	public static class ForTestsWebhooksLambdaFunction extends AWebhooksLambdaFunction {

		@Override
		protected Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input) {
			return Map.of("input", input);
		}
	};

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ForTestsWebhooksLambdaFunction lambdaFunction;

	@Test
	public void testOnSqsEvent() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> rawEvent = objectMapper
				.readValue(new ClassPathResource("/examples/lambda/sqs_event.json").getInputStream(), Map.class);

		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(rawEvent);

		Assertions.assertThat(output).hasSize(1);
		List<?> events = (List<?>) output.get("sqs");
		Assertions.assertThat(events).hasSize(1);

		Map<String, ?> firstEvent = (Map<String, ?>) events.get(0);
		IWebhookEvent inputEvent = (IWebhookEvent) firstEvent.get("input");
		Assertions.assertThat(inputEvent).isInstanceOf(GithubWebhookEvent.class);
	}

	@Test
	public void testOnDynamoDbEvent() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> rawEvent = objectMapper
				.readValue(new ClassPathResource("/examples/lambda/dynamodb_event.json").getInputStream(), Map.class);

		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(rawEvent);

		Assertions.assertThat(output).hasSize(1);
		List<?> events = (List<?>) output.get("sqs");
		Assertions.assertThat(events).hasSize(1);

		Map<String, ?> firstEvent = (Map<String, ?>) events.get(0);
		IWebhookEvent inputEvent = (IWebhookEvent) firstEvent.get("input");
		Assertions.assertThat(inputEvent).isInstanceOf(CleanThatWebhookEvent.class);
	}
}
