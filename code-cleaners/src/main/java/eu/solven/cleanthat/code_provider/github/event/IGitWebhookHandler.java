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
package eu.solven.cleanthat.code_provider.github.event;

import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * Knows how to process a Github webhook
 *
 * @author Benoit Lacelle
 */
public interface IGitWebhookHandler {
	/**
	 * Offline rejection of irrelevant events (e.g. the repo is starred by anybody). This should rely exclusively on
	 * webhook content: we do not hit the CodeProvider API at all
	 * 
	 * @param input
	 * @return
	 */
	GitWebhookRelevancyResult filterWebhookEventRelevant(I3rdPartyWebhookEvent input);

	/**
	 * Online rejection of irrelevant events (e.g. there is no configuration file in the concerned branch). This can
	 * rely on the CodeProvider API to checkout single specific files (e.g. cleanthat configuration). It shall not clone
	 * the report (or checkout a large number of individual files).
	 * 
	 * @param codeCleanerFactory
	 * 
	 * @param input
	 * @return
	 */
	WebhookRelevancyResult filterWebhookEventTargetRelevantBranch(ICodeCleanerFactory codeCleanerFactory,
			IWebhookEvent input);

	void doExecuteClean(ICodeCleanerFactory codeCleanerFactory, IWebhookEvent input);
}
