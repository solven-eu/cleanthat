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
package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.pepper.resource.PepperResourceHelper;

/**
 * This will help configuration CleanThat by proposing a reasonnable default configuration.
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanthatConfigInitializer implements ICleanthatConfigInitializer {
	public static final String TEMPLATES_FOLDER = "/templates";

	// This can be useful to be automatically notified on new PRs
	public static final String REF_TO_BLACELLE = "@solven-eu/cleanthat-notify please look at me";

	// final ICodeProvider codeProvider;
	final ObjectMapper objectMapper;
	final Collection<IEngineLintFixerFactory> factories;

	public CleanthatConfigInitializer(
			// ICodeProvider codeProvider,
			ObjectMapper objectMapper,
			Collection<IEngineLintFixerFactory> factories) {
		// this.codeProvider = codeProvider;
		this.objectMapper = objectMapper;
		this.factories = factories;
	}

	@Override
	public RepoInitializerResult prepareFile(ICodeProvider codeProvider, boolean isPrivate, String eventKey) {
		var defaultRepoPropertiesPath = ICleanthatConfigConstants.DEFAULT_PATH_CLEANTHAT;

		// Let's follow Renovate and its configuration PR
		// https://github.com/solven-eu/agilea/pull/1
		var body = PepperResourceHelper.loadAsString(TEMPLATES_FOLDER + "/onboarding-body.md");
		// body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), repo.getFullName());
		body = body.replaceAll(Pattern.quote("${DEFAULT_PATH}"), defaultRepoPropertiesPath);

		body = body.replaceAll(Pattern.quote("${EVENT_SOURCE}"), I3rdPartyWebhookEvent.X_GIT_HUB_DELIVERY);
		body = body.replaceAll(Pattern.quote("${EVENT_ID}"), eventKey);

		if (!isPrivate) {
			body += "\r\n" + "---" + "\r\n" + REF_TO_BLACELLE;
		}

		var commitMessage = PepperResourceHelper.loadAsString(TEMPLATES_FOLDER + "/commit-message.txt");
		var resultBuilder = RepoInitializerResult.builder().prBody(body).commitMessage(commitMessage);

		var generateInitialConfig = new GenerateInitialConfig(factories);
		try {
			var engineConfig = generateInitialConfig.prepareDefaultConfiguration(codeProvider);

			// Write the main config files (cleanthat.yaml)
			var repoPropertiesYaml = objectMapper.writeValueAsString(engineConfig.getRepoProperties());
			var repositoryRoot = codeProvider.getRepositoryRoot();
			resultBuilder.pathToContent(CleanthatPathHelpers.makeContentPath(repositoryRoot, defaultRepoPropertiesPath),
					repoPropertiesYaml);

			// Register the custom files of the engine
			engineConfig.getPathToContents()
					.forEach((k, v) -> resultBuilder
							.pathToContent(CleanthatPathHelpers.makeContentPath(repositoryRoot, k), v));
		} catch (IOException e) {
			throw new UncheckedIOException("Issue preparing initial config given codeProvider=" + codeProvider, e);
		}

		return resultBuilder.build();
	}

}
