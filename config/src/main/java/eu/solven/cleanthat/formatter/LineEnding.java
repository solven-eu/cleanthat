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
package eu.solven.cleanthat.formatter;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author marvin.froeder
 */
// https://github.com/revelc/formatter-maven-plugin/blob/master/src/main/java/net/revelc/code/formatter/LineEnding.java
public enum LineEnding {
	// https://docs.github.com/en/get-started/getting-started-with-git/configuring-git-to-handle-line-endings
	// https://git-scm.com/docs/git-config#Documentation/git-config.txt-coreeol
	NATIVE(System.lineSeparator()),
	// MacOS
	LF("\n"),
	// Windows
	CRLF("\r\n"),
	// Unix
	CR("\r"),
	// Request to keep current EOL, on a per file basis
	KEEP(null),
	// Rely on '.gitattributes' and similar mechanisms
	// It may maps different files to different EOL
	// If not '.gitattributes' is available, it would fallback to 'SYSTEM'
	GIT(null),
	// We keep this deprecated value else Jackson will fail early
	@Deprecated(forRemoval = true, since = "Replaced by NATIVE")
	AUTO(System.lineSeparator());

	private final String chars;

	LineEnding(String value) {
		this.chars = value;
	}

	/**
	 * 
	 * @return the actual characters for EOL, if determined. Else, null.
	 */
	public Optional<String> optChars() {
		return Optional.ofNullable(this.chars);
	}

	/**
	 * Returns the most occurring line-ending characters in the file text or null if no line-ending occurs the most.
	 */
	@SuppressWarnings({ "PMD.AvoidReassigningLoopVariables", "PMD.CognitiveComplexity" })
	public static Optional<LineEnding> determineLineEnding(String fileDataString) {
		var lfCount = 0;
		var crCount = 0;
		var crlfCount = 0;
		for (var i = 0; i < fileDataString.length(); i++) {
			var c = fileDataString.charAt(i);
			if (c == '\r') {
				if ((i + 1) < fileDataString.length() && fileDataString.charAt(i + 1) == '\n') {
					crlfCount++;
					i++;
				} else {
					crCount++;
				}
			} else if (c == '\n') {
				lfCount++;
			}
		}
		if (lfCount > crCount && lfCount > crlfCount) {
			return Optional.of(LF);
		} else if (crlfCount > lfCount && crlfCount > crCount) {
			return Optional.of(CRLF);
		} else if (crCount > lfCount && crCount > crlfCount) {
			return Optional.of(CR);
		}
		return Optional.empty();
	}

	public static String getOrGuess(LineEnding lineSeparator, Supplier<String> codeWithEol) {
		return lineSeparator.optChars()
				.or(() -> determineLineEnding(codeWithEol.get()).flatMap(LineEnding::optChars))
				// We fallback on the system EOL, which may be pointless if the input has no EOL (preventing EOL-guess)
				.orElse(LineEnding.NATIVE.optChars().get());
	}
}
