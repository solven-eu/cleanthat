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
package eu.solven.cleanthat.mvn;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

public class TestCleanThatInit {
	final FileSystem fs = Jimfs.newFileSystem();
	final CleanThatInitMojo mojo = new CleanThatInitMojo();

	{
		mojo.setFileSystem(fs);
	}

	final Path unrelatedPath = fs.getPath("/unrelated/directory/file.yml");

	@Test
	public void testConfigUrlIsNull() {
		Assertions.assertThatThrownBy(() -> mojo.getRepositoryConfigPath())
				.isInstanceOf(IllegalArgumentException.class)
				.message()
				.contains("must not be null");
	}

	@Test
	public void testConfigUrlIsAnything_doesNotExists() {
		mojo.setCleanthatRepositoryConfigPath("someDir/some.yaml");

		Assertions.assertThatThrownBy(() -> mojo.getRepositoryConfigPath())
				.isInstanceOf(IllegalArgumentException.class)
				.message()
				.contains("There is no configuration");
	}

	@Test
	public void testConfigUrlIsAnything_exists() throws IOException {
		// Where does this come from?
		String prefixFromMvnTests = "/work/";

		mojo.setCleanthatRepositoryConfigPath("someDir/some.yaml");
		var configPath = fs.getPath(prefixFromMvnTests, "someDir/some.yaml");
		Files.createDirectories(configPath.getParent());
		Files.writeString(configPath, "someContent");

		Assertions.assertThat(mojo.getRepositoryConfigPath().toString())
				.isEqualTo(prefixFromMvnTests + "someDir/some.yaml");
	}

	@Test
	public void testConfigUrlHasPlaceholder() {
		mojo.setCleanthatRepositoryConfigPath("${not_replaced}");
		Assertions.assertThatThrownBy(() -> mojo.getRepositoryConfigPath())
				.isInstanceOf(IllegalStateException.class)
				.message()
				.contains("placeholders", "${not_replaced}");
	}

	@Test
	public void testCheckIfValidToInit_unrelatedPath() {
		Assertions.assertThat(mojo.checkIfValidToInit(unrelatedPath)).isTrue();
	}

	@Test
	public void testCheckIfValidToInit_isDirectory() throws IOException {
		Files.createDirectories(unrelatedPath);

		Assertions.assertThat(mojo.checkIfValidToInit(unrelatedPath)).isFalse();
	}

	@Test
	public void testCheckIfValidToInit_hasContent() throws IOException {
		Files.createDirectories(unrelatedPath.getParent());
		Files.writeString(unrelatedPath, "existingContent");

		Assertions.assertThat(mojo.checkIfValidToInit(unrelatedPath)).isFalse();
	}
}
