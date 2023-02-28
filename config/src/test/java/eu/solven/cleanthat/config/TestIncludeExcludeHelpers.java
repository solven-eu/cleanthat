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
package eu.solven.cleanthat.config;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestIncludeExcludeHelpers {
	final FileSystem fs = FileSystems.getDefault();

	@Test
	public void testInvalidPath() {
		Assertions.assertThatThrownBy(() -> IncludeExcludeHelpers.prepareMatcher(fs, Arrays.asList("regex:notARegex(")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("notARegex");
	}

	@Test
	public void testAmbiguousDirectorySeparator() {
		var pathMatchers = IncludeExcludeHelpers.prepareMatcher(fs, Arrays.asList("regex:.*/do_not_format_me/.*"));

		// Under Windows: we would have a Windows PathMatcher, while we ensure the returned path holds '/' as directory
		// separator (e.g.
		// eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider.listFilesForContent(Consumer<ICodeProviderFile>))
		var optMatcher = IncludeExcludeHelpers.findMatching(pathMatchers,
				fs.getPath("/bash/src/main/resources/do_not_format_me/basic_raw.sh"));

		Assertions.assertThat(optMatcher).isPresent();
	}

	@Test
	public void testImplyGlob() {
		var pathMatchers = IncludeExcludeHelpers.prepareMatcher(fs, Arrays.asList("**/do_not_format_me/**"));

		// Under Windows: we would have a Windows PathMatcher, while we ensure the returned path holds '/' as directory
		// separator (e.g.
		// eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider.listFilesForContent(Consumer<ICodeProviderFile>))
		var optMatcher = IncludeExcludeHelpers.findMatching(pathMatchers,
				fs.getPath("/bash/src/main/resources/do_not_format_me/basic_raw.sh"));

		Assertions.assertThat(optMatcher).isPresent();
	}
}
