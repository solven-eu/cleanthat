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
package eu.solven.cleanthat.github.refs;

import java.io.IOException;

import org.junit.Test;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;

import eu.solven.cleanthat.IGitTestConstants;
import eu.solven.cleanthat.git_abstraction.GithubFacade;

public class TestGithubFacade implements IGitTestConstants {
	@Test
	public void testGetRef() throws IOException {
		GitHub github = Mockito.mock(GitHub.class);
		GHRepository repo = Mockito.mock(GHRepository.class);

		Mockito.when(github.getRepository(someRepoName)).thenReturn(repo);

		GithubFacade facade = new GithubFacade(github, someRepoName);

		facade.getRef(someBranchRef);

		Mockito.verify(repo).getRef("head/someBranchName");
	}
}
