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
