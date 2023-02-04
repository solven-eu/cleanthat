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

import eu.solven.cleanthat.code_provider.github.event.CompositeCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandler;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.CheckConfigWebhooksLambdaFunction;
import java.util.Map;

/**
 * Used to actually execute the cleaning
 * 
 * @author Benoit Lacelle
 *
 */
public class ExecuteCleaningWebhooksLambdaFunction extends AWebhooksLambdaFunction {
	// private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteCleaningWebhooksLambdaFunction.class);

	@Override
	protected Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input) {
		IGitWebhookHandler makeWithFreshJwt = CheckConfigWebhooksLambdaFunction.extracted(getAppContext());
		ICodeCleanerFactory cleanerFactory = getAppContext().getBean(CompositeCodeCleanerFactory.class);

		makeWithFreshJwt.doExecuteClean(cleanerFactory, input);

		return Map.of("whatever", "done");
	}

}
