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
package eu.solven.cleanthat.github.refs;

import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubCheckRunManager;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubRefCodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.IGitService;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;

public class TestGithubRefCleaner {
	@Test
	public void testGithubRefCleaner() throws IOException {
		GitHub gitHub = Mockito.mock(GitHub.class);

		GHRepository repository = Mockito.mock(GHRepository.class);
		Mockito.when(gitHub.getRepository("someUser/someRepoName")).thenReturn(repository);

		GHRef ref = Mockito.mock(GHRef.class);
		Mockito.when(repository.getRef("heads/someRef")).thenReturn(ref);

		GithubCheckRunManager checkRunManager = new GithubCheckRunManager(Mockito.mock(IGitService.class));
		GithubRefCleaner cleaner = new GithubRefCleaner(Arrays.asList(ConfigHelpers.makeJsonObjectMapper()),
				Arrays.asList(),
				Mockito.any(ICodeProviderFormatter.class),
				new GithubAndToken(gitHub, "someToken", Map.of()),
				checkRunManager);

		Path root = Files.createTempDirectory("cleanthat-TestGithubRefCleaner-");
		ICodeProvider codeProvider = cleaner.getCodeProviderForRef(root,
				new GitRepoBranchSha1("someUser/someRepoName", "refs/heads/someRef", "someSha1"));

		Assertions.assertThat(codeProvider).isInstanceOf(GithubRefCodeProvider.class);
	}
}
