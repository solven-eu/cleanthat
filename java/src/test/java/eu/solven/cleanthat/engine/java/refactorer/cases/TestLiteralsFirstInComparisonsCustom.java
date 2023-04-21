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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserTestCases;

public class TestLiteralsFirstInComparisonsCustom extends AJavaparserTestCases {
	final IJavaparserAstMutator mutator = new LiteralsFirstInComparisons();

	// Cannot keep element because we reached the end of nodetext
	@Test
	public void testIssueWithFile() throws IOException {
		Resource resource = new ClassPathResource("/source/do_not_format_me/MiTrust/TestNodeResourceImpl.java");
		var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

		var compilationUnit = parseCompilationUnit(mutator, asString);

		var transformed = mutator.walkAstHasChanged(compilationUnit);

		Assertions.assertThat(transformed).isTrue();
	}

	@Test
	public void testIssueWithFile2() throws IOException {
		Resource resource = new ClassPathResource("/source/do_not_format_me/Generic/ConstantWithDigitInName.java");
		var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

		var compilationUnit = parseCompilationUnit(mutator, asString);

		var transformed = mutator.walkAstHasChanged(compilationUnit);

		Assertions.assertThat(transformed).isFalse();
	}

	@Test
	public void testIssueWithFile3() throws IOException {
		Resource resource = new ClassPathResource("/source/do_not_format_me/MiTrust/LocaleHelper.java");
		var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

		var compilationUnit = parseCompilationUnit(mutator, asString);

		var transformed = mutator.walkAstHasChanged(compilationUnit);

		Assertions.assertThat(transformed).isFalse();
	}

	@Test
	public void testIssueWithUnresolvedSymbols() throws IOException {
		Resource resource = new ClassPathResource("/source/do_not_format_me/ShaftEngine/RecordManager.java");
		var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

		var compilationUnit = parseCompilationUnit(mutator, asString);

		var transformed = mutator.walkAstHasChanged(compilationUnit);
		Assertions.assertThat(transformed).isTrue();
	}
}
