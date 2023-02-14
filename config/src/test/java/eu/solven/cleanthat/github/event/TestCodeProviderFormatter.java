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
package eu.solven.cleanthat.github.event;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.config.IncludeExcludeHelpers;

// https://facelessuser.github.io/wcmatch/glob/
public class TestCodeProviderFormatter {
	final FileSystem fs = FileSystems.getDefault();

	@Test
	public void testMatchFile_root_absolute() {
		List<PathMatcher> matchers =
				IncludeExcludeHelpers.prepareMatcher(fs, IncludeExcludeHelpers.DEFAULT_INCLUDES_JAVA);

		Optional<PathMatcher> matching = IncludeExcludeHelpers.findMatching(matchers, fs.getPath("/SomeClass.java"));

		Assert.assertTrue(matching.isPresent());
	}

	// In this case, we suppose the issue would be to return a relative path
	@Test
	public void testMatchFile_root_relative() {
		List<PathMatcher> matchers = IncludeExcludeHelpers.prepareMatcher(FileSystems.getDefault(),
				IncludeExcludeHelpers.DEFAULT_INCLUDES_JAVA);

		Optional<PathMatcher> matching = IncludeExcludeHelpers.findMatching(matchers, fs.getPath("SomeClass.java"));

		Assert.assertTrue(matching.isPresent());
	}

	@Test
	public void testMatchFile_subFolder() {
		List<PathMatcher> matchers = IncludeExcludeHelpers.prepareMatcher(FileSystems.getDefault(),
				IncludeExcludeHelpers.DEFAULT_INCLUDES_JAVA);

		Optional<PathMatcher> matching =
				IncludeExcludeHelpers.findMatching(matchers, fs.getPath("src/main/java/SomeClass.java"));

		Assert.assertTrue(matching.isPresent());
	}
}
