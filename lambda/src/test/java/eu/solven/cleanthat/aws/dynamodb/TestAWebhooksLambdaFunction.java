package eu.solven.cleanthat.aws.dynamodb;

import java.io.IOException;
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
	public void testOnDynamoDbEvent() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> rawEvent = objectMapper
				.readValue(new ClassPathResource("/examples/lambda/dynamodb_event.json").getInputStream(), Map.class);

		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(rawEvent);

		Assertions.assertThat(output).hasSize(1);
	}

	@Test
	public void testOnSqsEvent() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> rawEvent = objectMapper
				.readValue(new ClassPathResource("/examples/lambda/sqs_event.json").getInputStream(), Map.class);

		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(rawEvent);

		Assertions.assertThat(output).hasSize(1);
	}
}
