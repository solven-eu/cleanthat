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
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers related to include and exclude rules
 * 
 * @author Benoit Lacelle
 *
 */
public class IncludeExcludeHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(IncludeExcludeHelpers.class);

	// It is good to know that '/' will be interpreted as folder separator even under Windows
	// https://stackoverflow.com/questions/9148528/how-do-i-use-directory-globbing-in-jdk7
	public static final List<String> DEFAULT_INCLUDES_JAVA = Arrays.asList("glob:**/*.java");

	protected IncludeExcludeHelpers() {
		// hidden
	}

	// https://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
	public static Optional<PathMatcher> findMatching(List<PathMatcher> includeMatchers, Path fileName) {
		return includeMatchers.stream().filter(pm -> pm.matches(fileName)).findFirst();
	}

	/**
	 * 
	 * @param globOrRegex
	 * @return a List of {@link PathMatcher}
	 */
	// https://stackoverflow.com/questions/44388227/sonar-raises-blocker-issue-on-java-filesystems-getdefault
	@SuppressWarnings("PMD.CloseResource")
	public static List<PathMatcher> prepareMatcher(FileSystem fs, Collection<String> globOrRegex) {
		return globOrRegex.stream().flatMap(r -> {

			if (!r.startsWith("glob:") && !r.startsWith("regex:")) {
				String newPattern = "glob:" + r;
				LOGGER.info("We implied glob from implicit syntax: {} -> {}", r, newPattern);
				r = newPattern;
			}

			// https://stackoverflow.com/questions/18722471/when-to-use-double-star-in-glob-syntax-within-java
			// In glob, the separator may be always '/'
			// Hence, replacement with '\\' would be necessary only under windows

			String newPattern;
			// https://stackoverflow.com/questions/64102053/java-pathmatcher-not-working-properly-on-windows
			if ("\\".equals(fs.getSeparator())) {
				// We are under Windows
				newPattern = r.replace("/", "\\\\");
				LOGGER.info("File.separator='{}' so we switched regex to: {}", fs.getSeparator(), newPattern);
			} else {
				// We are under Linux
				newPattern = r;
			}

			if (r.startsWith("glob:**/")) {
				// https://gitlab.com/gitlab-org/gitlab-foss/-/issues/66096
				// https://github.com/fish-shell/fish-shell/issues/7222
				String patternFromRoot = "glob:" + r.substring("glob:**/".length());
				return Stream.of(newPattern, patternFromRoot);
			} else {
				return Stream.of(newPattern);
			}

		}).map(r -> {
			try {
				return fs.getPathMatcher(r);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Invalid regex: " + r, e);
			}
		}).collect(Collectors.toList());
	}
}
