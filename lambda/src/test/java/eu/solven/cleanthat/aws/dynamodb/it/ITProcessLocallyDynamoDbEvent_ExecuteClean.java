/*
 * Copyright 2023-2024 Benoit Lacelle - SOLVEN
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
import org.springframework.test.context.junit4.SpringRunner;

import com.nimbusds.jose.JOSEException;

/**
 * This enables re-processing an event locally. Very useful to reproduce an issue, or test a know workload over a
 * different codebase
 * 
 * @author Benoit Lacelle
 *
 */
@RunWith(SpringRunner.class)
public class ITProcessLocallyDynamoDbEvent_ExecuteClean extends AProcessLocallyDynamoDbEvent_ExecuteClean {

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		// This is logged by: e.s.c.lambda.AWebhooksLambdaFunction|parseDynamoDbEvent
		// You can search logs for this key, in order to process given event locally
		var key = "random-54d3f147-5f0c-4f71-bc94-522f415e96cd";

		Map<String, ?> dynamoDbPureJson = EventFromDynamoDbITHelper.loadEvent("cleanthat_accepted_events", key);
		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(dynamoDbPureJson);

		Assertions.assertThat(output).hasSize(1);
	}
}
