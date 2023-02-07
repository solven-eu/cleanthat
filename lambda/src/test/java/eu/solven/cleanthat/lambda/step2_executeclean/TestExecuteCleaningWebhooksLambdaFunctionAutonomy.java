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
package eu.solven.cleanthat.lambda.step2_executeclean;

import com.google.common.collect.ImmutableMap;
import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.TestCheckConfigWebhooksLambdaFunctionAutonomy;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ExecuteCleaningWebhooksLambdaFunction.class,
		TestCheckConfigWebhooksLambdaFunctionAutonomy.CheckConfigWebhooksLambdaFunctionComplement.class })
public class TestExecuteCleaningWebhooksLambdaFunctionAutonomy {

	@Autowired
	ApplicationContext appContext;

	@Autowired
	ExecuteCleaningWebhooksLambdaFunction function;

	@Test
	public void testAppRun() {
		// When
		// {
		// IGitWebhookHandler gitWebhookHandler = appContext.getBean(IGitWebhookHandler.class);
		//
		// WebhookRelevancyResult value =
		// new WebhookRelevancyResult(Optional.empty(), Optional.of("someUnitTestReason"));
		// Mockito.when(
		// gitWebhookHandler.filterWebhookEventTargetRelevantBranch(Mockito.any(ICodeCleanerFactory.class),
		// Mockito.any(IWebhookEvent.class)))
		// .thenReturn(value);
		// }

		// Then
		Map<String, ?> payload = ImmutableMap.<String, Object>builder().build();
		Map<String, Object> headers = Map.of();
		function.unsafeProcessOneEvent(new CleanThatWebhookEvent(headers, payload));
	}
}
