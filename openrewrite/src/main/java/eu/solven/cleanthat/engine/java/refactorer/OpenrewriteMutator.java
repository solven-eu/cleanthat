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

import java.util.Arrays;
import java.util.Optional;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;

import com.google.common.collect.Iterables;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkableMutator;

/**
 * A {@link IMutator} configuring over an OpenRewrite {@link Recipe}
 * 
 * @author Benoit Lacelle
 *
 */
public class OpenrewriteMutator implements IWalkableMutator<J.CompilationUnit, Result> {
	final Recipe recipe;

	public OpenrewriteMutator(Recipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public Optional<Result> walkAst(CompilationUnit pre) {
		ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

		Result result = Iterables.getOnlyElement(recipe.run(Arrays.asList(pre), ctx).getResults());
		return Optional.of(result);
	}

}
