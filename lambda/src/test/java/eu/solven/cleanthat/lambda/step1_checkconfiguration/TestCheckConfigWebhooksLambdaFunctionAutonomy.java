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
package eu.solven.cleanthat.lambda.step1_checkconfiguration;

import com.google.common.collect.ImmutableMap;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CheckConfigWebhooksLambdaFunction.class,
		TestCheckConfigWebhooksLambdaFunctionAutonomy.CheckConfigWebhooksLambdaFunctionComplement.class })
public class TestCheckConfigWebhooksLambdaFunctionAutonomy {

	@Configuration
	public static class CheckConfigWebhooksLambdaFunctionComplement {

		@Primary
		@Bean
		public IGitWebhookHandler gitWebhookHandler() throws IOException {
			return Mockito.mock(IGitWebhookHandler.class);
		}

		// Override GithubWebhookHandlerFactory which will try actually connecting to GH
		@Primary
		@Bean
		public IGitWebhookHandlerFactory gitWebhookHandlerFactory(IGitWebhookHandler gitWebhookHandler)
				throws IOException {
			IGitWebhookHandlerFactory factory = Mockito.mock(IGitWebhookHandlerFactory.class);

			Mockito.when(factory.makeWithFreshAuth()).thenReturn(gitWebhookHandler);

			return factory;
		}
	}

	@Autowired
	ApplicationContext appContext;

	@Autowired
	CheckConfigWebhooksLambdaFunction function;

	@Test
	public void testAppRun() {
		// When
		{
			IGitWebhookHandler gitWebhookHandler = appContext.getBean(IGitWebhookHandler.class);

			WebhookRelevancyResult value =
					new WebhookRelevancyResult(Optional.empty(), Optional.of("someUnitTestReason"));
			Mockito.when(
					gitWebhookHandler.filterWebhookEventTargetRelevantBranch(Mockito.any(ICodeCleanerFactory.class),
							Mockito.any(IWebhookEvent.class)))
					.thenReturn(value);
		}

		// Then
		Map<String, ?> payload = ImmutableMap.<String, Object>builder().build();
		Map<String, Object> headers = Map.of();
		function.unsafeProcessOneEvent(new CleanThatWebhookEvent(headers, payload));
	}
}
