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
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHRepository;
import org.mockito.Mockito;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;

public class TestAGithubSha1CodeProvider {
	final String someRef = "someRef";
	final String someSha1 = "someSha1";

	@Test
	public void testRepoSha1AsZip() throws IOException {
		GHRepository ghRepo = Mockito.mock(GHRepository.class);

		var tmpZipFile = Files.createTempFile("cleanthat", "TestAGithubSha1CodeProvider.zip");
		tmpZipFile.toFile().delete();

		var root = tmpZipFile.getParent();
		AGithubSha1CodeProvider codeProvider = new AGithubSha1CodeProvider(root, "someToken", ghRepo) {

			@Override
			public String getSha1() {
				return someSha1;
			}

			@Override
			public String getRef() {
				return someRef;
			}
		};

		TestGithubCommitCodeProvider.mockHasZip(ghRepo, tmpZipFile, someSha1);

		var tmpDir = Files.createTempDirectory("cleanthat-TestAGithubSha1CodeProvider");
		ICodeProvider localCp = codeProvider.getHelper().downloadGitRefLocally(tmpDir);

		Set<String> paths = new HashSet<>();
		localCp.listFilesForContent(file -> paths.add(file.getPath().toString()));

		Assertions.assertThat(paths).contains("root.txt", "dir/toto.txt");

		Assertions.assertThat(localCp.loadContentForPath(CleanthatPathHelpers.makeContentPath(root, "root.txt")))
				.isPresent()
				.contains("someTiti");

		Assertions.assertThat(localCp.loadContentForPath(CleanthatPathHelpers.makeContentPath(root, "dir/toto.txt")))
				.isPresent()
				.contains("someToto");
	}
}
