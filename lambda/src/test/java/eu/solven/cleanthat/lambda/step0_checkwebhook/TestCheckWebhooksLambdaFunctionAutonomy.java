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
package eu.solven.cleanthat.lambda.step0_checkwebhook;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.TestCheckConfigWebhooksLambdaFunctionAutonomy;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CheckWebhooksLambdaFunction.class,
		TestCheckConfigWebhooksLambdaFunctionAutonomy.CheckConfigWebhooksLambdaFunctionComplement.class })
public class TestCheckWebhooksLambdaFunctionAutonomy {

	@Autowired
	ApplicationContext appContext;

	@Autowired
	CheckWebhooksLambdaFunction function;

	@Test
	public void testAppRun() {
		// When
		{
			IGitWebhookHandler gitWebhookHandler = appContext.getBean(IGitWebhookHandler.class);

			GitWebhookRelevancyResult value =
					new GitWebhookRelevancyResult(false, false, Optional.empty(), Optional.empty(), Optional.empty());
			Mockito.when(gitWebhookHandler.filterWebhookEventRelevant(Mockito.any(I3rdPartyWebhookEvent.class)))
					.thenReturn(value);
		}

		// Then
		Map<String, ?> payload = ImmutableMap.<String, Object>builder().build();
		function.unsafeProcessOneEvent(new GithubWebhookEvent(payload));
	}
}
