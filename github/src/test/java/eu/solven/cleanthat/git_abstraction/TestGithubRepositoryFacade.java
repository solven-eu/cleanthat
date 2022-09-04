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
