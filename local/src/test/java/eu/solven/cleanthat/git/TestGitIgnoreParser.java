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
package eu.solven.cleanthat.git;

import java.io.IOException;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.google.common.io.ByteStreams;

public class TestGitIgnoreParser {
	// https://github.com/codemix/gitignore-parser/blob/master/test/index.js
	@Test
	public void testCodeMix() throws IOException {
		String gitIgnoreContent = new String(ByteStreams
				.toByteArray(new ClassPathResource("/git/codemixGitIgnoreTestCase.gitignore").getInputStream()));

		Set<String> patterns = GitIgnoreParser.parsePatterns(gitIgnoreContent);

		Assertions.assertThat(patterns).isNotEmpty().contains("*.log", "/node_modules", "/nonexistent", "*.swp");

		// should accept the given filenames
		Assertions.assertThat(GitIgnoreParser.match(patterns, "test/index.js")).isFalse();
		Assertions.assertThat(GitIgnoreParser.match(patterns, "wat/test/index.js")).isFalse();

		// should not accept the given filenames
		Assertions.assertThat(GitIgnoreParser.match(patterns, "test.swp")).isTrue();
		Assertions.assertThat(GitIgnoreParser.match(patterns, "node_modules/wat.js")).isTrue();
		Assertions.assertThat(GitIgnoreParser.match(patterns, "foo/bar.wat")).isTrue();

		// should not accept the given directory
		Assertions.assertThat(GitIgnoreParser.match(patterns, "nonexistent")).isTrue();
		Assertions.assertThat(GitIgnoreParser.match(patterns, "nonexistent/bar")).isTrue();

		// should accept unignored files in ignored directories
		Assertions.assertThat(GitIgnoreParser.match(patterns, "nonexistent/foo")).isFalse();

		// hould accept nested unignored files in ignored directories
		Assertions.assertThat(GitIgnoreParser.match(patterns, "nonexistent/foo/wat")).isFalse();
	}

	@Test
	public void testGitDocumentation_hello() throws IOException {
		Set<String> helloStar = Set.of("hello.*");
		Assertions.assertThat(GitIgnoreParser.match(helloStar, "hello.")).isTrue();
		Assertions.assertThat(GitIgnoreParser.match(helloStar, "hello.alice")).isTrue();
		Assertions.assertThat(GitIgnoreParser.match(helloStar, "alice/hello.bob")).isTrue();

		Assertions.assertThat(GitIgnoreParser.match(helloStar, "hello")).isFalse();
		Assertions.assertThat(GitIgnoreParser.match(helloStar, "alice_hello.bob")).isFalse();
	}

	@Test
	public void testGitDocumentation_foo() throws IOException {
		Set<String> helloStar = Set.of("foo/");
		Assertions.assertThat(GitIgnoreParser.match(helloStar, "foo/bar")).isTrue();

		// Accepted as we assume input path are files, never directory
		Assertions.assertThat(GitIgnoreParser.match(helloStar, "foo")).isFalse();
	}
}
