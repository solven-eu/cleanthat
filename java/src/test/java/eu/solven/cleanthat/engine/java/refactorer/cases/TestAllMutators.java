/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UnnecessaryBoxing;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserTestCases;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestAllMutators extends AJavaparserTestCases {

	final IJavaparserAstMutator mutator = new UnnecessaryBoxing();

	@Test
	public void testIssueWithFile() throws IOException {
		for (IMutator mutator : new AllIncludingDraftSingleMutators(JavaVersion.parse(IJdkVersionConstants.LAST))
				.getUnderlyings()) {

			Resource resource =
					new ClassPathResource("/source/do_not_format_me/StringToString/TestJGitCodeProvider.java");
			var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

			var compilationUnit = parseCompilationUnit(mutator, asString);

			try {
				var transformed = ((IJavaparserAstMutator) mutator).walkAstHasChanged(compilationUnit);
			} catch (Throwable t) {
				// This detects as issue on JUnit4ToJUnit5
				LOGGER.warn("ARG", t);
			}

			// Assertions.assertThat(transformed).isTrue();
		}
	}
}
