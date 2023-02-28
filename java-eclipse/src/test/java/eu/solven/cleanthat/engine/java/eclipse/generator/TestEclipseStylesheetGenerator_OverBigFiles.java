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
package eu.solven.cleanthat.engine.java.eclipse.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.formatter.linewrap.WrapPreparator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

// Beware this test is very slow: any performance improvment would be welcome
public class TestEclipseStylesheetGenerator_OverBigFiles {
	final EclipseStylesheetGenerator generator = new EclipseStylesheetGenerator();

	final Map<String, Map<String, String>> defaultConfigs = generator.loadDefaultConfigurations();

	final OffsetDateTime inOneHour = OffsetDateTime.now().plusHours(1);

	@Before
	public void before() {
		// https://stackoverflow.com/questions/28901375/how-do-i-disable-java-assertions-for-a-junit-test-in-the-code
		// We encounter an unexpected assertion
		// TODO Report the issue to eclipse
		WrapPreparator.class.getClassLoader().setClassAssertionStatus(WrapPreparator.class.getName(), false);
	}

	@Test
	public void testRoaringBitmap() throws IOException {
		Resource testRoaringBitmapSource =
				new ClassPathResource("/source/do_not_format_me/RoaringBitmap/RoaringBitmap.java");
		var asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		Map<Path, String> pathToFile = Map.of(Paths.get("/RoaringBitmap.java"), asString);

		ScoredOption<Map<String, String>> defaultConf = generator.findBestDefaultSetting(inOneHour, pathToFile);

		// Check we consider eclipse java is the best default configuration
		{
			Assertions.assertThat(defaultConfigs.entrySet()
					.stream()
					.filter(e -> e.getValue().equals(defaultConf.getOption()))
					.map(java.util.Map.Entry::getKey)
					.findAny()
					.get()).isEqualTo("google");
		}

		{
			Map<String, String> settings =
					generator
							.optimizeSetOfSettings(inOneHour,
									pathToFile,
									defaultConf,
									Set.of("org.eclipse.jdt.core.formatter.tabulation.char",
											"org.eclipse.jdt.core.formatter.tabulation.size"))
							.getOption();

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "2")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "100");
		}

		// All settings
		// TODO Very slow
		if (false) {
			Map<String, String> settings = generator.generateSettings(inOneHour, pathToFile);

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "2")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "240");
		}
	}

	@Test
	public void testTestRoaringBitmap() throws IOException {
		Resource testRoaringBitmapSource =
				new ClassPathResource("/source/do_not_format_me/RoaringBitmap/TestRoaringBitmap.java");
		var asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		Map<Path, String> pathToFile = Map.of(Paths.get("/TestRoaringBitmap.java"), asString);

		ScoredOption<Map<String, String>> defaultConf = generator.findBestDefaultSetting(inOneHour, pathToFile);

		// Check we consider eclipse java is the best default configuration
		{
			Assertions.assertThat(defaultConfigs.entrySet()
					.stream()
					.filter(e -> e.getValue().equals(defaultConf.getOption()))
					.map(java.util.Map.Entry::getKey)
					.findAny()
					.get()).isEqualTo("default");
		}

		{
			Map<String, String> settings =
					generator
							.optimizeSetOfSettings(inOneHour,
									pathToFile,
									defaultConf,
									Set.of("org.eclipse.jdt.core.formatter.tabulation.char",
											"org.eclipse.jdt.core.formatter.tabulation.size"))
							.getOption();

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "4")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "120");
		}

		// All settings
		// TODO Very slow
		if (false) {
			Map<String, String> settings = generator.generateSettings(inOneHour, pathToFile);

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "2")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "240");
		}
	}
}
