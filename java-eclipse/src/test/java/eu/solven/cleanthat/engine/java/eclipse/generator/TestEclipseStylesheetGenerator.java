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
import java.util.Collection;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

public class TestEclipseStylesheetGenerator {
	final EclipseStylesheetGenerator generator = new EclipseStylesheetGenerator() {
		// In this class, we do not want to rely on Eclipse actual formatting
		@Override
		protected ScoredOption<Map<String, String>> computeScore(Collection<String> contents,
				Map<String, String> tweakedConfiguration) {

			// Do not keep the worstScore here, so that some configuration can be preferred over the initial
			// configuration
			long score = Long.MAX_VALUE - 1;
			return new ScoredOption<Map<String, String>>(tweakedConfiguration, score);
		};
	};

	@Test
	public void testInterruptEarly() throws IOException {
		Resource testRoaringBitmapSource =
				new ClassPathResource("/source/do_not_format_me/RoaringBitmap/RoaringBitmap.java");
		String asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		Map<Path, String> pathToFile = Map.of(Paths.get("/RoaringBitmap.java"), asString);

		// This should stop ASAP
		generator.generateSettings(OffsetDateTime.now().minusHours(1), pathToFile);
	}

	// Some default configuration lacks some parameters (e.g. later introduced by Eclipse)
	// We need to ensure we consider all parameters in the optimization process
	@Test
	public void testFromEmptyConfiguration() throws IOException {
		Resource testRoaringBitmapSource =
				new ClassPathResource("/source/do_not_format_me/RoaringBitmap/RoaringBitmap.java");
		String asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		Map<Path, String> pathToFile = Map.of(Paths.get("/RoaringBitmap.java"), asString);

		ScoredOption<Map<String, String>> optimalConf = generator.optimizeSetOfSettings(OffsetDateTime.now()
				.plusHours(1), pathToFile, new ScoredOption<>(Map.of(), Long.MAX_VALUE), generator.getAllSetttings());

		// The fake generator will allow a single conf to be considered better: hence, we verify here we add any
		// parameter into the configuration, while it was not present in the original configuration
		Assertions.assertThat(optimalConf.getOption())
				.hasSize(1)
				.containsKey("org.eclipse.jdt.core.formatter.align_assignment_statements_on_columns");
	}
}
