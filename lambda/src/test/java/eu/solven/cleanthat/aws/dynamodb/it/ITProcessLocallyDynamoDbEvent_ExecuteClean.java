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
		String key = "random-75ef3135-825d-4900-8b8b-bac22ae50a92";

		Map<String, ?> dynamoDbPureJson = EventFromDynamoDbITHelper.loadEvent("cleanthat_accepted_events", key);
		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(dynamoDbPureJson);

		Assertions.assertThat(output).hasSize(1);
	}
}
