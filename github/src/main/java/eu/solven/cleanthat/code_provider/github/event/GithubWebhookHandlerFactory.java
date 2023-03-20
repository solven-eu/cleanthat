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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.kohsuke.github.GitHub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * Factory for {@link GitHub}, on a per-installation basis
 *
 * @author Benoit Lacelle
 */
public class GithubWebhookHandlerFactory implements IGitWebhookHandlerFactory {
	final IGithubAppFactory githubAppFactory;

	final List<ObjectMapper> objectMappers;

	final GithubCheckRunManager githubCheckRunManager;

	public GithubWebhookHandlerFactory(IGithubAppFactory githubAppFactory,
			List<ObjectMapper> objectMappers,
			GithubCheckRunManager githubCheckRunManager) {
		this.githubAppFactory = githubAppFactory;
		this.objectMappers = objectMappers;
		this.githubCheckRunManager = githubCheckRunManager;
	}

	@Override
	public IGitWebhookHandler makeNoAuth() throws IOException {
		GithubNoApiWebhookHandler underlying = makeUnderlyingNoAuth();
		return new IGitWebhookHandler() {

			@Override
			public GitWebhookRelevancyResult filterWebhookEventRelevant(I3rdPartyWebhookEvent input) {
				return underlying.filterWebhookEventRelevant(input);
			}

			@Override
			public WebhookRelevancyResult filterWebhookEventTargetRelevantBranch(ICodeCleanerFactory codeCleanerFactory,
					IWebhookEvent input) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void doExecuteClean(ICodeCleanerFactory codeCleanerFactory, IWebhookEvent input) {
				throw new UnsupportedOperationException();
			}
		};
	}

	public GithubNoApiWebhookHandler makeUnderlyingNoAuth() {
		GithubNoApiWebhookHandler underlying = new GithubNoApiWebhookHandler(objectMappers);
		return underlying;
	}

	@Override
	public IGitWebhookHandler makeWithFreshAuth() throws IOException {
		GithubWebhookHandler githubWebhookHandler = makeGithubWebhookHandler();

		var noAuth = makeNoAuth();

		return new IGitWebhookHandler() {
			@Override
			public GitWebhookRelevancyResult filterWebhookEventRelevant(I3rdPartyWebhookEvent input) {
				return noAuth.filterWebhookEventRelevant(input);
			}

			@Override
			public WebhookRelevancyResult filterWebhookEventTargetRelevantBranch(ICodeCleanerFactory codeCleanerFactory,
					IWebhookEvent input) {
				try (var fs = Jimfs.newFileSystem()) {
					return githubWebhookHandler.filterWebhookEventTargetRelevantBranch(fs.getPath(fs.getSeparator()),
							codeCleanerFactory,
							input);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			@Override
			public void doExecuteClean(ICodeCleanerFactory codeCleanerFactory, IWebhookEvent input) {
				// We make a FileSystem per ICodeProvider
				try (var fs = Jimfs.newFileSystem()) {
					githubWebhookHandler.doExecuteClean(fs.getPath(fs.getSeparator()), codeCleanerFactory, input);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
	}

	public GithubWebhookHandler makeGithubWebhookHandler() throws IOException {
		return new GithubWebhookHandler(githubAppFactory, objectMappers, githubCheckRunManager);
	}
}
