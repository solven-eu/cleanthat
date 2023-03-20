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

import org.kohsuke.github.GitHub;

import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.utils.ResultOrError;

/**
 * Knows how to implement a {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubAppFactory {
	/**
	 * Beware this is not specific to a given installation. It enables listing all installations.
	 * 
	 * @return
	 */
	GitHub makeAppGithub();

	/**
	 * 
	 * @param installationId
	 * @return a {@link GitHub} instance authenticated as given installation, having access to permitted repositories
	 */
	ResultOrError<GithubAndToken, WebhookRelevancyResult> makeInstallationGithub(long installationId);
}
