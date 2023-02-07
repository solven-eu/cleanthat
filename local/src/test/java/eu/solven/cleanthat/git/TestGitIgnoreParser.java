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
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "test/index.js")).isTrue();
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "wat/test/index.js")).isTrue();

		// should not accept the given filenames
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "test.swp")).isFalse();
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "node_modules/wat.js")).isFalse();
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "foo/bar.wat")).isFalse();

		// should not accept the given directory
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "nonexistent")).isFalse();
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "nonexistent/bar")).isFalse();

		// should accept unignored files in ignored directories
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "nonexistent/foo")).isTrue();

		// hould accept nested unignored files in ignored directories
		Assertions.assertThat(GitIgnoreParser.accept(patterns, "nonexistent/foo/wat")).isTrue();
	}

	@Test
	public void testGitDocumentation_hello() throws IOException {
		Set<String> helloStar = Set.of("hello.*");
		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "hello.")).isFalse();
		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "hello.alice")).isFalse();
		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "alice/hello.bob")).isFalse();

		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "hello")).isTrue();
		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "alice_hello.bob")).isTrue();
	}

	@Test
	public void testGitDocumentation_foo() throws IOException {
		Set<String> helloStar = Set.of("foo/");
		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "foo/bar")).isFalse();

		// Accepted as we assume input path are files, never directory
		Assertions.assertThat(GitIgnoreParser.accept(helloStar, "foo")).isTrue();
	}
}
