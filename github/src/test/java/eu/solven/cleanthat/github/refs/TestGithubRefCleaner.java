package eu.solven.cleanthat.github.refs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.event.GithubAndToken;
import eu.solven.cleanthat.github.event.pojo.GitRepoBranchSha1;

public class TestGithubRefCleaner {
	@Test
	public void testGithubRefCleaner() throws IOException {
		GitHub gitHub = Mockito.mock(GitHub.class);

		GHRepository repository = Mockito.mock(GHRepository.class);
		Mockito.when(gitHub.getRepository("someRepoName")).thenReturn(repository);

		GHRef ref = Mockito.mock(GHRef.class);
		Mockito.when(repository.getRef("heads/someRef")).thenReturn(ref);

		GithubRefCleaner cleaner = new GithubRefCleaner(Arrays.asList(ConfigHelpers.makeJsonObjectMapper()),
				Mockito.any(ICodeProviderFormatter.class),
				new GithubAndToken(gitHub, "someToken", Map.of()));

		ICodeProvider codeProvider =
				cleaner.getCodeProviderForRef(new GitRepoBranchSha1("someRepoName", "refs/heads/someRef", "someSha1"));

		Assertions.assertThat(codeProvider).isInstanceOf(GithubRefCodeProvider.class);
	}
}
