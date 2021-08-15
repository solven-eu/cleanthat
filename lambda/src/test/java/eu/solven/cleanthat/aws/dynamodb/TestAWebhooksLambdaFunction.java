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

import eu.solven.cleanthat.aws.dynamodb.TestAWebhooksLambdaFunction.FortestsWebhooksLambdaFunction;
import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { FortestsWebhooksLambdaFunction.class })
public class TestAWebhooksLambdaFunction {
	public static class FortestsWebhooksLambdaFunction extends AWebhooksLambdaFunction {

		@Override
		protected Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input) {
			return Map.of("input", input);
		}
	};

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	FortestsWebhooksLambdaFunction lambdaFunction;

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
