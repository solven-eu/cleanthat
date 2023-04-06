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
package eu.solven.cleanthat.engine.java.refactorer.cases;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.engine.java.refactorer.mutators.StringReplaceAllWithQuotableInput;

public class TestStringReplaceAllWithQuotableInputCustom {

	@Test
	public void testRegex() {
		// We double the `\\` as we process regex from java source-code, hence `\` are already escaped

		// '.' needs escaping
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\.")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex(".")).isFalse();
		// '(' needs escaping
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\(")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("(")).isFalse();
		// ':' needs escaping
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\:")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex(":")).isFalse();
		// '}' needs escaping
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\}")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("}")).isFalse();

		// '\\\\\\\\' is escaping the escaping character
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\\\\\")).isTrue();

		// Word characters can not be escaped, else they have a special meaning
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("a")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\a")).isFalse();

		// Some special characters may or may not be escaped
		// `_` has a special meaning given our crafted based on `\w`
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\_")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("_")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("\\\\%")).isTrue();
		Assertions.assertThat(StringReplaceAllWithQuotableInput.isQuotableRegex("%")).isTrue();
	}
}