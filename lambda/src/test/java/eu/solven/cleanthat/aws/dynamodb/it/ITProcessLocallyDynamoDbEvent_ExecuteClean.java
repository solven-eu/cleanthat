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
import eu.solven.cleanthat.lambda.step2_executeclean.ExecuteCleaningWebhooksLambdaFunction;

/**
 * This enables re-processing an event locally. Very useful to reproduce an issue, or test a know workload over a
 * different codebase
 * 
 * @author Benoit Lacelle
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ExecuteCleaningWebhooksLambdaFunction.class })
public class ITProcessLocallyDynamoDbEvent_ExecuteClean {
	@Autowired
	AWebhooksLambdaFunction lambdaFunction;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		// This is logged by: e.s.c.lambda.AWebhooksLambdaFunction|parseDynamoDbEvent
		// You can search logs for this key, in order to process given event locally
		String key = "random-48b9507c-e7c1-45cd-b328-629ffd141110";

		Map<String, ?> dynamoDbPureJson = EventFromDynamoDbITHelper.loadEvent("cleanthat_accepted_events", key);
		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(dynamoDbPureJson);

		Assertions.assertThat(output).hasSize(1);
	}
}
