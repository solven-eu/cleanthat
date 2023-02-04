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
package local;

import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestFileSystemCodeProvider {
	final File tmpFolder = org.assertj.core.util.Files.newTemporaryFolder();

	@Test
	public void testLoadAbsolutePathAsRelative() throws IOException {
		Path tmpFolderAsPath = tmpFolder.toPath();
		FileSystemCodeProvider codeProvider = new FileSystemCodeProvider(tmpFolderAsPath);

		// Consider a file at the root of given folder
		Files.writeString(tmpFolderAsPath.resolve("cleanthat.yml"), "something");

		Optional<String> optContent = codeProvider.loadContentForPath("/cleanthat.yml");

		Assertions.assertThat(optContent).isPresent().contains("something");
	}

	@Test
	public void testLoadRelativePath() throws IOException {
		Path tmpFolderAsPath = tmpFolder.toPath();
		FileSystemCodeProvider codeProvider = new FileSystemCodeProvider(tmpFolderAsPath);

		// Consider a file at the root of given folder
		Files.writeString(tmpFolderAsPath.resolve("cleanthat.yml"), "something");

		Assertions.assertThatThrownBy(() -> codeProvider.loadContentForPath("cleanthat.yml"))
				.isInstanceOf(IllegalArgumentException.class);

		Assertions.assertThatThrownBy(() -> codeProvider.loadContentForPath("./cleanthat.yml"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testLoadNotExistingFile() throws IOException {
		Path tmpFolderAsPath = tmpFolder.toPath();
		FileSystemCodeProvider codeProvider = new FileSystemCodeProvider(tmpFolderAsPath);

		// Consider a file at the root of given folder
		Files.writeString(tmpFolderAsPath.resolve("cleanthat.yml"), "something");

		Optional<String> optContent = codeProvider.loadContentForPath("/doesNotExist.yml");

		Assertions.assertThat(optContent).isEmpty();
	}
}
