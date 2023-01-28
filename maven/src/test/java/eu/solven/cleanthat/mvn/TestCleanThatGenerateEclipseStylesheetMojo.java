/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.mvn;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.SystemUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;

public class TestCleanThatGenerateEclipseStylesheetMojo {
	final CleanThatGenerateEclipseStylesheetMojo mojo = new CleanThatGenerateEclipseStylesheetMojo();

	@Test
	public void testNortmalizeEclipsePath() {
		Path normalized = mojo.relativizeFromGitRootAsFSRoot(
				Paths.get("/Users/blacelle/workspace2/RoaringBitmap/.cleanthat/eclipse_formatter-stylesheet.xml"),
				Paths.get("/Users/blacelle/workspace2/RoaringBitmap/cleanthat.yaml"));

		Assertions.assertThat(normalized.getFileName().toString()).isEqualTo("eclipse_formatter-stylesheet.xml");
		Assertions.assertThat(CleanThatGenerateEclipseStylesheetMojo.toString(normalized))
				.isEqualTo("/.cleanthat/eclipse_formatter-stylesheet.xml");
	}

	@Test
	public void testNortmalizeEclipsePath_windows() {
		Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
		Path normalized = mojo.relativizeFromGitRootAsFSRoot(
				Paths.get(
						"C:\\Users\\blacelle\\workspace2\\RoaringBitmap\\.cleanthat\\eclipse_formatter-stylesheet.xml"),
				Paths.get("C:\\Users\\blacelle\\workspace2\\RoaringBitmap\\cleanthat.yaml"));

		Assertions.assertThat(normalized.getFileName().toString()).isEqualTo("eclipse_formatter-stylesheet.xml");
		Assertions.assertThat(CleanThatGenerateEclipseStylesheetMojo.toString(normalized))
				.isEqualTo("/.cleanthat/eclipse_formatter-stylesheet.xml");
	}
}
