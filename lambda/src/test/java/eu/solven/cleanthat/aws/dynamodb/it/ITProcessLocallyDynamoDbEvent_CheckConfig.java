package eu.solven.cleanthat.aws.dynamodb.it;

import java.io.IOException;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.CheckConfigWebhooksLambdaFunction;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CheckConfigWebhooksLambdaFunction.class })
public class ITProcessLocallyDynamoDbEvent_CheckConfig {
	@Autowired
	AWebhooksLambdaFunction lambdaFunction;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		String key = "random-d1f94a67-ffe5-4334-9e78-b47e21486581";

		Map<String, ?> output = lambdaFunction.ingressRawWebhook()
				.apply(EventFromDynamoDbITHelper.loadEvent("cleanthat_webhooks_github", key));

		Assertions.assertThat(output).hasSize(1);
	}
}
