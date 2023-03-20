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
package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRef.GHObject;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.GithubCheckRunManager;
import eu.solven.cleanthat.code_provider.github.event.GithubCodeCleaner;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.IGithubAppFactory;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.decorator.ILazyGitReference;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.HeadAndOptionalBase;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.IGitService;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.ICleanthatGitRefsConstants;

public class TestGithubWebhookHandler {

	private IGithubAppFactory ghFactory = Mockito.mock(IGithubAppFactory.class, Mockito.RETURNS_DEEP_STUBS);

	final GithubWebhookHandler handler = new GithubWebhookHandler(ghFactory,
			Arrays.asList(ConfigHelpers.makeJsonObjectMapper()),
			new GithubCheckRunManager(Mockito.mock(IGitService.class)));

	final ObjectMapper objectMapper = new ObjectMapper();

	final GHRepository repo = Mockito.mock(GHRepository.class);
	final GithubRepositoryFacade facade = new GithubRepositoryFacade(repo);

	final GithubCodeCleaner refCleaner =
			new GithubCodeCleaner(Paths.get(this.getClass().getSimpleName()), Mockito.mock(IGitRefCleaner.class));

	final String someRepoFullName = "someOrg/someRepo";

	{
		Mockito.when(facade.getRepoFullName()).thenReturn(someRepoFullName);
	}

	@Test
	public void testPrepareHeadSuppler() throws JsonParseException, JsonMappingException, IOException {
		var refName = "refs/someRef";
		var headToClean = new GitRepoBranchSha1(someRepoFullName, refName, "someSha1");
		ILazyGitReference result = refCleaner
				.prepareHeadSupplier(
						new WebhookRelevancyResult(Optional.of(new HeadAndOptionalBase(headToClean, Optional.empty())),
								Optional.empty()),
						repo,
						facade,
						new AtomicReference<>());

		Assertions.assertThat(result.getFullRefOrSha1()).isEqualTo("someSha1");

		GHObject someRefObject = Mockito.mock(GHObject.class);
		Mockito.when(someRefObject.getSha()).thenReturn("someSha1");

		GHRef someRef = Mockito.mock(GHRef.class);
		Mockito.when(someRef.getObject()).thenReturn(someRefObject);
		Mockito.when(someRef.getRef()).thenReturn(refName);

		Mockito.when(repo.getRef("someRef")).thenReturn(someRef);

		var supplier = result.getSupplier().get();

		Assertions.assertThat(supplier.getFullRefOrSha1()).isIn(refName, "someSha1");
		Assertions.assertThat(supplier.<GHRef>getDecorated()).isEqualTo(someRef);
	}

	@Test
	public void testPrepareHeadSuppler_cleanthatOwnBranch()
			throws JsonParseException, JsonMappingException, IOException {
		var refName = ICleanthatGitRefsConstants.PREFIX_REF_CLEANTHAT_TMPHEAD + "someBranch/someCLeanForToday";
		var refNameWithRefsPrefix = refName.substring("refs/".length());

		Mockito.when(repo.getRef(refNameWithRefsPrefix)).thenThrow(new GHFileNotFoundException("Not materialized yet"));

		ILazyGitReference result = refCleaner.prepareHeadSupplier(new WebhookRelevancyResult(
				Optional.of(new HeadAndOptionalBase(new GitRepoBranchSha1(someRepoFullName, refName, "someSha1"),
						Optional.empty())),
				Optional.empty()), repo, new GithubRepositoryFacade(repo), new AtomicReference<>());

		Assertions.assertThat(result.getFullRefOrSha1()).isEqualTo("someSha1");

		GHRef someRef = Mockito.mock(GHRef.class);
		Mockito.when(someRef.getRef()).thenReturn(refName);
		Mockito.when(repo.createRef(refName, "someSha1")).thenReturn(someRef);

		var supplier = result.getSupplier().get();

		Assertions.assertThat(supplier.getFullRefOrSha1()).isIn(refName, "someSha1");
		Assertions.assertThat(supplier.<GHRef>getDecorated()).isEqualTo(someRef);
	}

	// In case of re-run
	@Test
	public void testPrepareHeadSuppler_cleanthatOwnBranch_alreadyExists()
			throws JsonParseException, JsonMappingException, IOException {
		var refName = ICleanthatGitRefsConstants.PREFIX_REF_CLEANTHAT_TMPHEAD + "someBranch/someCLeanForToday";
		var refNameWithRefsPrefix = refName.substring("refs/".length());

		GHObject someRefObject = Mockito.mock(GHObject.class);
		Mockito.when(someRefObject.getSha()).thenReturn("someSha1");

		GHRef someRef = Mockito.mock(GHRef.class);
		Mockito.when(repo.getRef(refNameWithRefsPrefix)).thenReturn(someRef);

		Mockito.when(someRef.getObject()).thenReturn(someRefObject);
		Mockito.when(someRef.getRef()).thenReturn(refName);

		ILazyGitReference result = refCleaner.prepareHeadSupplier(new WebhookRelevancyResult(
				Optional.of(new HeadAndOptionalBase(new GitRepoBranchSha1(someRepoFullName, refName, "someSha1"),
						Optional.empty())),
				Optional.empty()), repo, new GithubRepositoryFacade(repo), new AtomicReference<>());

		Assertions.assertThat(result.getFullRefOrSha1()).isEqualTo("someSha1");

		var supplier = result.getSupplier().get();

		Assertions.assertThat(supplier.getFullRefOrSha1()).isIn(refName, "someSha1");
		Assertions.assertThat(supplier.<GHRef>getDecorated()).isEqualTo(someRef);

		Mockito.verify(repo, Mockito.never()).createRef(Mockito.anyString(), Mockito.anyString());
	}
}
