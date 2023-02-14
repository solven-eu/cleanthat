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

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very limitted implementation of a .gitgnore parser. It is very limitted as it handles only a very small subset of
 * .gitignore rules
 * 
 * @author Benoit Lacelle
 *
 */
// Inspired by https://github.com/codemix/gitignore-parser/blob/master/lib/index.js
// https://git-scm.com/docs/gitignore
public class GitIgnoreParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitIgnoreParser.class);

	protected GitIgnoreParser() {
		// hidden
	}

	public static Set<String> parsePatterns(String gitIgnore) {
		// TODO: Handle negative patterns (i.e. starting with '!')
		return Stream.of(gitIgnore.split("[\r\n]+"))
				// A blank line matches no files, so it can serve as a separator for readability.
				.filter(s -> !s.isBlank())
				.map(s -> s.trim())
				// A line starting with # serves as a comment.
				.filter(s -> !s.startsWith("#"))
				.collect(Collectors.toSet());
	}

	public static boolean match(Set<String> patterns, Path path) {
		Set<String> ignoredPatterns = patterns.stream().filter(s -> !s.startsWith("!")).collect(Collectors.toSet());
		boolean doMatch = doMatch(ignoredPatterns, path);

		if (doMatch) {
			Set<String> unignoredPatterns = patterns.stream()
					// Filter unignored patterns
					.filter(s -> s.startsWith("!"))
					// Remove the leading '!'
					.map(s -> s.substring(1, s.length()))
					.collect(Collectors.toSet());
			boolean doMatchUnignored = doMatch(unignoredPatterns, path);

			if (doMatchUnignored) {
				// Matches unignored: it is accepted
				return false;
			}

			// It is rejected
			return true;
		} else {
			// Not ignored: it is accepted
			return false;
		}
	}

	// https://git-scm.com/docs/gitignore
	private static boolean doMatch(Set<String> patterns, Path p) {
		return patterns.stream().flatMap(s -> {
			Set<String> allowedPatterns = new TreeSet<>();

			// We do not require prefix '**/' as we may hit the root
			allowedPatterns.add(s);

			if (!s.startsWith("/")) {
				// This enables working with gitignore entries not starting with '/', hence accepting any parent folder
				allowedPatterns.add("**/" + s);
			}

			return allowedPatterns.stream();
			// }).map(s -> {
			// if (!s.endsWith("**")) {
			// // Any gitignore entries
			// s += "**";
			// }
			//
			// return s;
		}).flatMap(s -> {
			// https://git-scm.com/docs/gitignore#_pattern_format
			// If there is a separator at the end of the pattern then the pattern will only match directories, otherwise
			// the pattern can match both files and directories.
			if (s.endsWith("/")) {
				if (Files.isDirectory(p)) {
					// We add the directory as its own file: it will reject file named like directory (which is bad but
					// rate)
					// But it will enable rejecting a directory as soon as we encounter it
					String directoryAsFile = s.substring(0, s.length() - 1);
					return Stream.of(directoryAsFile);
				} else {
					s = s + "**";
					return Stream.of(s);
				}
			} else {
				// Accept any children
				return Stream.of(s, s + "/**");
			}
		}).anyMatch(s -> {
			Path pToTEst;
			FileSystem fs = p.getFileSystem();
			if (s.startsWith("/") && !p.isAbsolute()) {
				pToTEst = fs.getPath(fs.getSeparator()).resolve(p);
			} else {
				pToTEst = p;
			}

			// https://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
			boolean matches = fs.getPathMatcher("glob:" + s).matches(pToTEst);
			if (matches) {
				LOGGER.trace("{} accepted {}", s, p);
			}
			return matches;
			// return Pattern.matches(s, path);
		});
	}
}
