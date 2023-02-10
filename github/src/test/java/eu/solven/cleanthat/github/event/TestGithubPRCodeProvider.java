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

import eu.solven.cleanthat.code_provider.github.refs.GithubPRCodeProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

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
		String content = new GithubPRCodeProvider(FileSystems.getDefault(), "someToken", "someEventKey", pr)
				.loadContentForPath(file.getFilename())
				.get();
		Assert.assertEquals("someContent", content);
	}
}
