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

import java.nio.file.FileSystem;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;

public class TestDummyCodeProviderFile {
	final FileSystem fs = Jimfs.newFileSystem();

	@Test
	public void testIsAbsolute() {
		Assertions.assertThatThrownBy(() -> new DummyCodeProviderFile(fs.getPath("/dir/file"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testTrailingDoubleSlash() {
		// Considered like an absolute path
		Assertions.assertThatThrownBy(() -> new DummyCodeProviderFile(fs.getPath("//dir/file"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testRelative() {
		new DummyCodeProviderFile(fs.getPath("dir/file"), null).getPath();
	}
}
