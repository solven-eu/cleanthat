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
package eu.solven.cleanthat.code_provider.inmemory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

public class TestFileSystemCodeProvider {
	@Test
	public void testInMemoryFileSystem() throws IOException {
		FileSystem fs = MemoryFileSystemBuilder.newEmpty().build();
		ICodeProviderWriter cp = new FileSystemCodeProvider(fs.getPath("/"));

		cp.listFilesForContent(file -> {
			Assertions.fail("The FS is empty");
		});

		cp.persistChanges(Map.of(fs.getPath("/").resolve(fs.getPath("root", "directory", "file.txt")), "newContent"),
				Arrays.asList(),
				Collections.emptyList());

		cp.listFilesForContent(file -> {
			Assertions.assertThat(file.getPath()).isEqualTo("/root/directory/file.txt");

			Path raw = (Path) file.getRaw();
			try {
				Assertions.assertThat(Files.readString(raw, StandardCharsets.UTF_8)).isEqualTo("newContent");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
}
