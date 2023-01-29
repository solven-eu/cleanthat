/*
 * Copyright 2023 Solven
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

import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.JOSEException;
import eu.solven.cleanthat.lambda.step0_checkwebhook.CheckWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.CheckConfigWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step2_executeclean.ExecuteCleaningWebhooksLambdaFunction;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This enables processing similarly that on a push event over given branch
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated(since = "NotReady: it is complicated to craft GitHub events manually (installation.id, after.sha1, ...)")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CheckWebhooksLambdaFunction.class,
		CheckConfigWebhooksLambdaFunction.class,
		ExecuteCleaningWebhooksLambdaFunction.class })
public class ITProcessLocallyOverPushBranch {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITProcessLocallyOverPushBranch.class);

	@Autowired
	CheckWebhooksLambdaFunction checkEvent;
	@Autowired
	CheckConfigWebhooksLambdaFunction checkConfig;
	@Autowired
	ExecuteCleaningWebhooksLambdaFunction cleanCode;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		Map<String, ?> githubEvent =
				ImmutableMap.<String, Object>builder().put("installation", Map.of("id", 123L)).build();
		Map<String, ?> checkEventOutput = checkEvent.ingressRawWebhook().apply(githubEvent);
		Map<String, ?> checkConfigOutput = checkConfig.ingressRawWebhook().apply(checkEventOutput);
		Map<String, ?> cleanCodeOutput = cleanCode.ingressRawWebhook().apply(checkConfigOutput);

		LOGGER.info("Output: {}", cleanCodeOutput);
	}
}
