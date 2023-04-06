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
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.codeprovider.CodeWritingMetadata;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.pepper.unittest.ILogDisabler;
import eu.solven.pepper.unittest.PepperTestHelper;

public class TestFileSystemCodeProvider {
	final FileSystem fs = Jimfs.newFileSystem();

	@Test
	public void testInMemoryFileSystem() throws IOException {
		ICodeProviderWriter cp = new FileSystemCodeProvider(fs.getPath(fs.getSeparator()));

		cp.listFilesForContent(file -> Assertions.fail("The FS is empty"));

		cp.persistChanges(Map.of(fs.getPath("root", "directory", "file.txt"), "newContent"),
				CodeWritingMetadata.empty());

		cp.listFilesForContent(file -> {
			Assertions.assertThat(file.getPath().toString()).isEqualTo("root/directory/file.txt");

			var raw = (Path) file.getRaw();
			try {
				Assertions.assertThat(Files.readString(raw, StandardCharsets.UTF_8)).isEqualTo("newContent");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Test
	public void testLoadFileOutOfRoot() throws IOException {
		var secretPath = fs.getPath(fs.getSeparator(), "secretFile");
		Files.writeString(secretPath, "secretContent");

		var notSecretPath = fs.getPath(fs.getSeparator(), "project", "notSecretFile");
		Files.createDirectories(notSecretPath.getParent());
		Files.writeString(notSecretPath, "notSecretContent");

		ICodeProviderWriter cp = new FileSystemCodeProvider(fs.getPath(fs.getSeparator(), "project"));

		Assertions.assertThatThrownBy(() -> cp.loadContentForPath(secretPath))
				.isInstanceOf(IllegalArgumentException.class);

		Assertions
				.assertThatThrownBy(
						() -> cp.loadContentForPath(fs.getPath(fs.getSeparator(), "project", "..", "secretFile")))
				.isInstanceOf(IllegalArgumentException.class);

		Assertions.assertThat(cp.loadContentForPath(cp.getRepositoryRoot().relativize(notSecretPath)))
				.contains("notSecretContent");
		var illegalLookingValid = fs.getPath(fs.getSeparator(), "project", "..", "project", "notSecretFile");
		Assertions.assertThat(illegalLookingValid.normalize()).isEqualTo(notSecretPath);
		Assertions.assertThatThrownBy(() -> cp.loadContentForPath(illegalLookingValid))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testReadWrongCharset() throws IOException {
		var utf8File = fs.getPath(fs.getSeparator(), "project", "someUtf8File");
		var asciiFile = fs.getPath(fs.getSeparator(), "project", "someAciiFile");

		// https://www.baeldung.com/java-string-encode-utf-8
		var germanString = "Entwickeln Sie mit Vergnügen";

		Files.createDirectories(utf8File.getParent());
		Files.writeString(utf8File, germanString);

		var germanBytes = germanString.getBytes();
		var asciiEncodedString = new String(germanBytes, StandardCharsets.US_ASCII);

		Files.createDirectories(asciiFile.getParent());
		Files.writeString(asciiFile, asciiEncodedString);

		{
			ICodeProviderWriter utfCodeProvider =
					new FileSystemCodeProvider(fs.getPath(fs.getSeparator(), "project"), StandardCharsets.UTF_8);

			Optional<String> optAscii = utfCodeProvider.loadContentForPath("someAciiFile");
			Assertions.assertThat(optAscii)
					.isPresent()
					.get()
					.asString()
					.isNotEqualTo(germanString)
					.startsWith("Entwickeln")
					.endsWith("gen");

			Optional<String> optUtf8 = utfCodeProvider.loadContentForPath("someUtf8File");
			Assertions.assertThat(optUtf8).isPresent().get().asString().isEqualTo(germanString);
		}

		try (ILogDisabler logDisabler = PepperTestHelper.disableLog(FileSystemCodeProvider.class)) {
			ICodeProviderWriter utfCodeProvider =
					new FileSystemCodeProvider(fs.getPath(fs.getSeparator(), "project"), StandardCharsets.US_ASCII);

			// This may throws as ascii can not read some UTF-8 codepoints
			Optional<String> optAscii = utfCodeProvider.loadContentForPath("someAciiFile");
			Assertions.assertThat(optAscii).isEmpty();

			// This may throws as ascii can not read some UTF-8 codepoints
			Optional<String> optUtf8 = utfCodeProvider.loadContentForPath("someUtf8File");
			Assertions.assertThat(optUtf8).isEmpty();
		}
	}
}
