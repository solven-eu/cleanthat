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
package eu.solven.cleanthat.git_abstraction;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

public class TestGithubRepositoryFacade {
	@Test
	public void testRepositoryName() {
		GHRepository repository = Mockito.mock(GHRepository.class);

		Mockito.when(repository.getFullName()).thenReturn("someOrg/someRepoName");

		GithubRepositoryFacade facade = new GithubRepositoryFacade(repository);

		Assertions.assertThat(facade.getRepoFullName()).isEqualTo("someOrg/someRepoName");
	}
}
