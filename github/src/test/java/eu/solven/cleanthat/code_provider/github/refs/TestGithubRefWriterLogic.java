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
package eu.solven.cleanthat.code_provider.github.refs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import com.google.common.jimfs.Jimfs;

public class TestGithubRefWriterLogic {

	final FileSystem fs = Jimfs.newFileSystem();

	final GHRepository repo = Mockito.mock(GHRepository.class);
	final GHRef ref = Mockito.mock(GHRef.class, Mockito.RETURNS_DEEP_STUBS);
	final GHCompare ghCompare = Mockito.mock(GHCompare.class);

	final String someRefName = "someRefName";

	// The content which has been pushed
	final String someReadSha1 = "readSha1";
	// The content in the ref when processing the event, if changed since push
	final String someFreshSha1 = "someFreshSha1";
	// The content in the ref when committing the event, if changed since processing the event
	final String someFresherSha1 = "someFresherSha1";

	final String someFileName = "folder/sub/file.ext";
	final String otherFileName = "folder/sub/other.ext";

	@Test
	public void RejectConflictingChanges_empty() {
		Mockito.when(ref.getObject().getSha()).thenReturn(someReadSha1);

		GithubRefWriterLogic writerLogic = new GithubRefWriterLogic("someEventKey", repo, ref, someReadSha1);

		Map<Path, String> pathToMutatedContent = Map.of();
		Map<Path, String> filtered =
				writerLogic.filterOutPathsHavingDiverged(pathToMutatedContent, someRefName, someFresherSha1);

		Assertions.assertThat(filtered).isEmpty();
	}

	@Test
	public void RejectConflictingChanges_noConflict() throws IOException {
		Mockito.when(ref.getObject().getSha()).thenReturn(someFreshSha1);

		Mockito.when(repo.getCompare(someReadSha1, someFreshSha1)).thenReturn(ghCompare);

		GHCommit.File editedFile = Mockito.mock(GHCommit.File.class);
		Mockito.when(editedFile.getFileName()).thenReturn(otherFileName);
		Mockito.when(ghCompare.getFiles()).thenReturn(new GHCommit.File[] { editedFile });

		GithubRefWriterLogic writerLogic = new GithubRefWriterLogic("someEventKey", repo, ref, someReadSha1);

		Map<Path, String> pathToMutatedContent = Map.of(fs.getPath(someFileName), "someCleanContent");
		Map<Path, String> filtered =
				writerLogic.filterOutPathsHavingDiverged(pathToMutatedContent, someRefName, someFreshSha1);

		Assertions.assertThat(filtered).containsEntry(fs.getPath(someFileName), "someCleanContent");
	}

	@Test
	public void RejectConflictingChanges_conflictAdd() throws IOException {
		Mockito.when(ref.getObject().getSha()).thenReturn(someFreshSha1);

		Mockito.when(repo.getCompare(someReadSha1, someFreshSha1)).thenReturn(ghCompare);

		GHCommit.File editedFile = Mockito.mock(GHCommit.File.class);
		Mockito.when(editedFile.getFileName()).thenReturn(someFileName);
		Mockito.when(ghCompare.getFiles()).thenReturn(new GHCommit.File[] { editedFile });

		GithubRefWriterLogic writerLogic = new GithubRefWriterLogic("someEventKey", repo, ref, someReadSha1);

		Map<Path, String> pathToMutatedContent = Map.of(fs.getPath(someFileName), "someCleanContent");
		Map<Path, String> filtered =
				writerLogic.filterOutPathsHavingDiverged(pathToMutatedContent, someRefName, someFreshSha1);

		Assertions.assertThat(filtered).isEmpty();
	}

	@Test
	public void RejectConflictingChanges_renamedFrom() throws IOException {
		Mockito.when(ref.getObject().getSha()).thenReturn(someFreshSha1);

		Mockito.when(repo.getCompare(someReadSha1, someFreshSha1)).thenReturn(ghCompare);

		GHCommit.File editedFile = Mockito.mock(GHCommit.File.class);
		Mockito.when(editedFile.getFileName()).thenReturn(otherFileName);
		Mockito.when(editedFile.getPreviousFilename()).thenReturn(someFileName);
		Mockito.when(ghCompare.getFiles()).thenReturn(new GHCommit.File[] { editedFile });

		GithubRefWriterLogic writerLogic = new GithubRefWriterLogic("someEventKey", repo, ref, someReadSha1);

		Map<Path, String> pathToMutatedContent = Map.of(fs.getPath(someFileName), "someCleanContent");
		Map<Path, String> filtered =
				writerLogic.filterOutPathsHavingDiverged(pathToMutatedContent, someRefName, someFreshSha1);

		Assertions.assertThat(filtered).isEmpty();
	}

	@Test
	public void RejectConflictingChanges_renamedTo() throws IOException {
		Mockito.when(ref.getObject().getSha()).thenReturn(someFreshSha1);

		Mockito.when(repo.getCompare(someReadSha1, someFreshSha1)).thenReturn(ghCompare);

		GHCommit.File editedFile = Mockito.mock(GHCommit.File.class);
		Mockito.when(editedFile.getFileName()).thenReturn(someFileName);
		Mockito.when(editedFile.getPreviousFilename()).thenReturn(otherFileName);
		Mockito.when(ghCompare.getFiles()).thenReturn(new GHCommit.File[] { editedFile });

		GithubRefWriterLogic writerLogic = new GithubRefWriterLogic("someEventKey", repo, ref, someReadSha1);

		Map<Path, String> pathToMutatedContent = Map.of(fs.getPath(someFileName), "someCleanContent");
		Map<Path, String> filtered =
				writerLogic.filterOutPathsHavingDiverged(pathToMutatedContent, someRefName, someFreshSha1);

		Assertions.assertThat(filtered).isEmpty();
	}
}
