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
package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import com.google.common.collect.Sets;

import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;

public class TestGithubHelper {
	final GHRepository repo = Mockito.mock(GHRepository.class);
	final GHBranch branch = Mockito.mock(GHBranch.class);

	@Test
	public void testDefaultBranch_explicit() throws IOException {
		Mockito.when(repo.getDefaultBranch()).thenReturn("someBranch");
		Mockito.when(repo.getBranch("someBranch")).thenReturn(branch);

		Assertions.assertThat(GithubHelper.getDefaultBranch(repo)).isEqualTo(branch);
	}

	protected void mockOtherBranchNamesAsUnknown() {
		Sets.difference(Set.copyOf(CleanthatRefFilterProperties.SIMPLE_DEFAULT_BRANCHES), Set.of(branch.getName()))
				.forEach(branch -> {
					try {
						Mockito.when(repo.getBranch(branch)).thenThrow(GHFileNotFoundException.class);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}

	@Test
	public void testDefaultBranch_implicitMaster() throws IOException {
		Mockito.when(branch.getName()).thenReturn("master");
		Mockito.when(repo.getBranch("master")).thenReturn(branch);

		mockOtherBranchNamesAsUnknown();

		Assertions.assertThat(GithubHelper.getDefaultBranch(repo)).isEqualTo(branch);

	}

	@Test
	public void testDefaultBranch_implicitMain() throws IOException {
		Mockito.when(branch.getName()).thenReturn("main");
		Mockito.when(repo.getBranch("main")).thenReturn(branch);

		mockOtherBranchNamesAsUnknown();

		Assertions.assertThat(GithubHelper.getDefaultBranch(repo)).isEqualTo(branch);
	}

	// Consider the case of not branch materializing the default branch
	@Test
	public void testDefaultBranch_explicit_explicitIsMissing() throws IOException {
		// There is an explicit default branch name, but no branch for it
		Mockito.when(repo.getDefaultBranch()).thenReturn("someBranch");
		Mockito.when(repo.getBranch("someBranch")).thenThrow(GHFileNotFoundException.class);

		Mockito.when(branch.getName()).thenReturn("master");
		Mockito.when(repo.getBranch("master")).thenReturn(branch);

		mockOtherBranchNamesAsUnknown();

		Assertions.assertThat(GithubHelper.getDefaultBranch(repo)).isEqualTo(branch);
	}

	@Test
	public void testDefaultBranch_noneExists() throws IOException {
		// There is a branch, unrelated to default branches
		Mockito.when(repo.getBranch("otherBranch")).thenReturn(branch);
		Mockito.when(branch.getName()).thenReturn("otherBranch");

		Mockito.when(repo.getDefaultBranch()).thenReturn("someBranch");
		Mockito.when(repo.getBranch("someBranch")).thenThrow(GHFileNotFoundException.class);

		mockOtherBranchNamesAsUnknown();

		Assertions.assertThatThrownBy(() -> GithubHelper.getDefaultBranch(repo))
				.isInstanceOf(IllegalStateException.class);
	}
}
