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
package eu.solven.cleanthat.engine.java.refactorer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;

public class TestAAstRefactorer {

	final String someAst = "someAst";
	final String someParser = "someParser";
	final String someResult = "someResult";

	final String someResultAsString = "someResultAsString";
	final String someInvalidResultAsString = "someInvalidResultAsString";
	final String otherResultAsString = "otherResultAsString";

	final IWalkingMutator<String, String> someValidMutator = Mockito.mock(IWalkingMutator.class);
	final IWalkingMutator<String, String> someInvalidMutator = Mockito.mock(IWalkingMutator.class);
	final IWalkingMutator<String, String> otherValidMutator = Mockito.mock(IWalkingMutator.class);

	@Test
	public void testRejectInvalidTransformedCode_validInvalidValid() throws IOException {
		List<IWalkingMutator<String, String>> mutators =
				Arrays.asList(someValidMutator, someInvalidMutator, otherValidMutator);
		AAstRefactorer<String, String, String, IWalkingMutator<String, String>> refactorer = makeRefactorer(mutators);

		String inputJavaCode = "someInputJavaCode";

		Mockito.when(someValidMutator.walkAst(inputJavaCode)).thenReturn(Optional.of(someResultAsString));
		Mockito.when(someInvalidMutator.walkAst(someResultAsString)).thenReturn(Optional.of(someInvalidResultAsString));
		Mockito.when(otherValidMutator.walkAst(someResultAsString)).thenReturn(Optional.of(otherResultAsString));

		var outputCode = refactorer.applyTransformers(inputJavaCode);

		Assertions.assertThat(outputCode).isEqualTo(otherResultAsString);
	}

	@Test
	public void testRejectInvalidTransformedCode_validInvalid() throws IOException {
		List<IWalkingMutator<String, String>> mutators = Arrays.asList(someValidMutator, someInvalidMutator);
		AAstRefactorer<String, String, String, IWalkingMutator<String, String>> refactorer = makeRefactorer(mutators);

		String inputJavaCode = "someInputJavaCode";

		Mockito.when(someValidMutator.walkAst(inputJavaCode)).thenReturn(Optional.of(someResultAsString));
		Mockito.when(someInvalidMutator.walkAst(someResultAsString)).thenReturn(Optional.of(someInvalidResultAsString));

		var outputCode = refactorer.applyTransformers(inputJavaCode);

		Assertions.assertThat(outputCode).isEqualTo(someResultAsString);
	}

	private AAstRefactorer<String, String, String, IWalkingMutator<String, String>> makeRefactorer(
			List<IWalkingMutator<String, String>> mutators) {
		AAstRefactorer<String, String, String, IWalkingMutator<String, String>> refactorer =
				new AAstRefactorer<String, String, String, IWalkingMutator<String, String>>(mutators) {

					@Override
					public String doFormat(String content) throws IOException {
						return someResult;
					}

					@Override
					public String getId() {
						return "mockito";
					}

					@Override
					protected String makeAstParser() {
						return someParser;
					}

					@Override
					protected Optional<String> parseSourceCode(String parser, String sourceCode) {
						return Optional.of(sourceCode);
					}

					@Override
					protected String toString(String walkResult) {
						return walkResult;
					}

					@Override
					protected boolean isValidResultString(String parser, String resultAsString) {
						return Set.of(someResultAsString, otherResultAsString).contains(resultAsString);
					}
				};
		return refactorer;
	}
}
