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
package eu.solven.cleanthat.jgit;

import java.nio.file.Paths;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Test;
import org.mockito.Mockito;

import eu.solven.cleanthat.codeprovider.ICodeProviderFile;

public class TestJGitCodeProvider {

	@Test
	public void testAcceptPath() {
		JGitCodeProvider codeProvider = new JGitCodeProvider(Paths.get("any"), Mockito.mock(Git.class), "someSha1");

		Consumer<ICodeProviderFile> consumer = file -> {
			Assertions.assertThat(file.getPath()).startsWith("/").isEqualTo("/root/folder/file");
		};
		TreeWalk treeWalk = Mockito.mock(TreeWalk.class);
		Mockito.when(treeWalk.getPathString()).thenReturn("root/folder/file");
		codeProvider.acceptLocalTreeWalk(consumer, treeWalk);
	}

	@Test
	public void testResolvePath() {
		JGitCodeProvider codeProvider =
				new JGitCodeProvider(Paths.get("/git_root/git_folder"), Mockito.mock(Git.class), "someSha1");

		Assertions.assertThat(codeProvider.resolvePath("/root/folder/file").toString().replace('\\', '/'))
				.isEqualTo("/git_root/git_folder/root/folder/file");
	}
}
