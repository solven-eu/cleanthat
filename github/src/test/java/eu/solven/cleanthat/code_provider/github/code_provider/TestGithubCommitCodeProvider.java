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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.function.InputStreamFunction;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;

import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubCommitCodeProvider;

public class TestGithubCommitCodeProvider {
	final String someRef = "someRef";
	final String someSha1 = "someSha1";

	final FileSystem fs = Jimfs.newFileSystem();

	@Test
	public void testRepoSha1AsZip() throws IOException {
		GHRepository ghRepo = Mockito.mock(GHRepository.class);

		var tmpZipFile = Files.createTempFile("cleanthat", "TestAGithubSha1CodeProvider.zip");
		tmpZipFile.toFile().delete();

		GHCommit commit = Mockito.mock(GHCommit.class);
		Mockito.when(commit.getSHA1()).thenReturn(someSha1);

		GithubCommitCodeProvider codeProvider =
				new GithubCommitCodeProvider(fs.getPath("/root/dir"), "someToken", ghRepo, commit);

		mockHasZip(ghRepo, tmpZipFile, someSha1);

		// var tmpDir = Files.createTempDirectory("cleanthat-TestAGithubSha1CodeProvider");
		// ICodeProvider localCodeProvider = codeProvider.getHelper().downloadGitRefLocally(tmpDir);

		List<GHTreeEntry> treeEntries = Mockito.mock(List.class);

		// Simulate a large repository: It will force downloading as a Zip
		Mockito.when(treeEntries.size()).thenReturn(123_456);

		GHTree ghTree = Mockito.mock(GHTree.class);
		Mockito.when(ghTree.getTree()).thenReturn(treeEntries);

		Mockito.when(ghRepo.getTreeRecursive(someSha1, 1)).thenReturn(ghTree);

		{
			Set<String> paths = new HashSet<>();
			codeProvider.listFilesForContent(file -> paths.add(file.getPath().toString()));

			Assertions.assertThat(paths).contains("root.txt", "dir/toto.txt");
		}

		Assertions
				.assertThat(codeProvider.loadContentForPath(
						CleanthatPathHelpers.makeContentPath(codeProvider.getRepositoryRoot(), "root.txt")))
				.isPresent()
				.contains("someTiti");

		Assertions
				.assertThat(codeProvider.loadContentForPath(
						CleanthatPathHelpers.makeContentPath(codeProvider.getRepositoryRoot(), "dir/toto.txt")))
				.isPresent()
				.contains("someToto");
	}

	public static void mockHasZip(GHRepository ghRepo, Path tmpZipFile, String sha1) throws IOException {
		// https://github.com/google/jimfs
		// https://stackoverflow.com/questions/44459152/provider-not-found-exception-when-creating-a-filesystem-for-my-zip
		{
			Map<String, String> env = new HashMap<>();

			// https://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemproviderprops.html
			// Create the zip file if it doesn't exist
			env.put("create", "true");

			// The 'jar:' prefix is necessary to enable the use of FileSystems.newFileSystem over a .zip
			var uri = URI.create("jar:" + tmpZipFile.toUri());

			try (var zipfs = FileSystems.newFileSystem(uri, env)) {

				Files.createDirectory(zipfs.getPath("/repository_branch_random"));

				// Path externalTxtFile = Paths.get("/codeSamples/zipfs/SomeTextFile.txt");
				{
					var pathInZipfile = zipfs.getPath("/repository_branch_random/root.txt");

					Files.copy(new ByteArrayInputStream("someTiti".getBytes(StandardCharsets.UTF_8)), pathInZipfile);
				}

				{
					var pathInZipfile = zipfs.getPath("/repository_branch_random/dir/toto.txt");

					Files.createDirectories(pathInZipfile.getParent());

					Files.copy(new ByteArrayInputStream("someToto".getBytes(StandardCharsets.UTF_8)), pathInZipfile);
				}
			}
		}

		Mockito.when(ghRepo.readZip(Mockito.any(InputStreamFunction.class), Mockito.eq(sha1))).then(invok -> {
			InputStreamFunction<Object> isf = invok.getArgument(0);

			return isf.apply(new FileSystemResource(tmpZipFile).getInputStream());
		});
	}
}
