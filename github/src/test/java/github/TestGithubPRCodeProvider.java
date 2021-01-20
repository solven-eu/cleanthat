package github;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.github.event.GithubPRCodeProvider;

public class TestGithubPRCodeProvider {

	@Test
	public void testLoadFile() throws IOException {
		GHPullRequest pr = Mockito.mock(GHPullRequest.class);
		GHRepository repository = Mockito.mock(GHRepository.class);
		GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
		Mockito.when(pr.getRepository()).thenReturn(repository);
		Mockito.when(pr.getHead()).thenReturn(head);
		Mockito.when(head.getSha()).thenReturn("headSha");
		GHPullRequestFileDetail file = Mockito.mock(GHPullRequestFileDetail.class);
		Mockito.when(file.getFilename()).thenReturn("someFileName");
		GHContent ghContent = Mockito.mock(GHContent.class);
		Mockito.when(repository.getFileContent("someFileName", "headSha")).thenReturn(ghContent);
		Mockito.when(ghContent.read())
				.thenReturn(new ByteArrayInputStream("someContent".getBytes(StandardCharsets.UTF_8)));
		String content =
				new GithubPRCodeProvider("someToken", pr).deprecatedLoadContent(new DummyCodeProviderFile(file));
		Assert.assertEquals("someContent", content);
	}
}
