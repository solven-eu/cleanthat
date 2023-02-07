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
package eu.solven.cleanthat.mvn;

import eu.solven.cleanthat.engine.java.eclipse.generator.EclipseStylesheetGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class TestEclipseStylesheet {
	final EclipseStylesheetGenerator generator = new EclipseStylesheetGenerator();
	final CleanThatGenerateEclipseStylesheetMojo mojo = new CleanThatGenerateEclipseStylesheetMojo();

	{
		mojo.setJavaRegex(CleanThatGenerateEclipseStylesheetMojo.DEFAULT_JAVA_REGEX);
	}

	@Test
	public void testLoadFiles() {

		final String path = "./src/test/java/";

		Map<Path, String> contents = mojo.loadAnyJavaFile(Set.of(), generator, Set.of(Paths.get(path)));

		Assertions.assertThat(contents)
				.containsKey(Path.of("./src/test/java/eu/solven/cleanthat/mvn/TestEclipseStylesheet.java"));
	}

	// Typically on module with no 'src/main/java'
	@Test
	public void testLoadFiles_doesNotExist() {
		final String path = "./src/test/java/does_not_exists";

		Map<Path, String> contents = mojo.loadAnyJavaFile(Set.of(), generator, Set.of(Paths.get(path)));

		Assertions.assertThat(contents).isEmpty();
	}

	@Test
	public void testWriteSettings_exist() throws IOException {
		Path tmpPath = Files.createTempFile("cleanthat-TestEclipseStylesheet", ".xml");

		mojo.setConfigPath(tmpPath.toString());
		mojo.writeSettings(Map.of());
	}

	@Test
	public void testWriteSettings_doesNotExist_inSubFolder() throws IOException {
		Path tmpPath = Files.createTempDirectory("cleanthat-TestEclipseStylesheet");

		boolean deleted = tmpPath.toFile().delete();
		Assert.assertTrue(deleted);

		mojo.setConfigPath(tmpPath.resolve("eclipse-stylesheet.xml").toString());
		mojo.writeSettings(Map.of());
	}
}
