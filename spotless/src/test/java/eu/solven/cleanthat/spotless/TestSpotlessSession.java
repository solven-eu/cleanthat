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
package eu.solven.cleanthat.spotless;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

public class TestSpotlessSession {
	final SpotlessSession ss = new SpotlessSession();

	@Test
	public void testPrepareFile_defaultFs_absolute() {
		Assertions.assertThatThrownBy(() -> ss.getFakeFile(Paths.get("/otherRoot"), Paths.get("/root/some/file")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testPrepareFile_defaultFs_relative() {
		File someFile = ss.getFakeFile(Paths.get("/root"), Paths.get("some/file"));
		Assertions.assertThat(someFile).doesNotExist().isAbsolute();

		// BEWAR: here, 'spotless' is the name of current module
		Assertions.assertThat(someFile.toString()).isEqualTo("/root/some/file");
	}

	@Test
	public void testPrepareFile_memoryFs() {
		FileSystem fs = Jimfs.newFileSystem();

		File someFile = ss.getFakeFile(fs.getPath("/root"), fs.getPath("some/file"));
		Assertions.assertThat(someFile).doesNotExist().isAbsolute();
		Assertions.assertThat(someFile.toString())
				.startsWith("/cleanthat_fake_root_for_spotless/")
				.endsWith("/some/file");
	}
}
