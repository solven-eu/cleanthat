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
package eu.solven.cleanthat.engine.openrewrite;

import java.util.Arrays;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.openrewrite.Changeset;
import org.openrewrite.ExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;

import eu.solven.cleanthat.engine.java.refactorer.AAstRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteMutator;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteRefactorer;

public class TestOpenrewriteMutator {
	final String someClassContent = "package eu.solven.cleanthat.do_not_format_me;" + "public class SomeClass {"
			+ "	public SomeClass() {"
			+ "		{}"
			+ "		;;"
			+ "	}"
			+ "}";

	final Recipe recipe = Mockito.mock(Recipe.class);
	final RecipeRun recipeRun = Mockito.mock(RecipeRun.class);
	final Changeset changeSet = Mockito.mock(Changeset.class);

	{
		Mockito.when(recipeRun.getChangeset()).thenReturn(changeSet);
	}

	@Test
	public void testEmptyResult() {
		OpenrewriteMutator mutator = new OpenrewriteMutator(recipe);

		final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());
		SourceFile pre = AAstRefactorer.parse(refactorer, someClassContent).get();

		Mockito.doAnswer(i -> recipeRun)
				.when(recipe)
				.run(Mockito.any(LargeSourceSet.class), Mockito.any(ExecutionContext.class));

		Optional<Result> output = mutator.walkAst(pre);

		Assertions.assertThat(output).isEmpty();
	}

	@Test
	public void testExistingResult() {
		Recipe recipe = Mockito.mock(Recipe.class);
		OpenrewriteMutator mutator = new OpenrewriteMutator(recipe);

		final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());
		SourceFile pre = AAstRefactorer.parse(refactorer, someClassContent).get();

		Mockito.doAnswer(i -> recipeRun)
				.when(recipe)
				.run(Mockito.any(LargeSourceSet.class), Mockito.any(ExecutionContext.class));

		Optional<Result> output = mutator.walkAst(pre);

		Assertions.assertThat(output).isEmpty();
	}

	@Test
	public void testDiscardChangeIfAnyException() {
		Recipe recipe = Mockito.mock(Recipe.class);
		OpenrewriteMutator mutator = new OpenrewriteMutator(recipe);

		final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());
		SourceFile pre = AAstRefactorer.parse(refactorer, someClassContent).get();

		Mockito.doAnswer(i -> {
			ExecutionContext ec = i.getArgument(1, ExecutionContext.class);
			ec.getOnError().accept(new RuntimeException("Something went bad during the process"));

			return recipeRun;
		}).when(recipe).run(Mockito.any(LargeSourceSet.class), Mockito.any(ExecutionContext.class));

		Optional<Result> output = mutator.walkAst(pre);

		Assertions.assertThat(output).isEmpty();
	}
}
