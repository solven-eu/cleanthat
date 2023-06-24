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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;

/**
 * A {@link IMutator} configuring over an OpenRewrite {@link Recipe}
 * 
 * @author Benoit Lacelle
 *
 */
public class OpenrewriteMutator implements IWalkingMutator<SourceFile, Result> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenrewriteMutator.class);

	final Recipe recipe;

	public OpenrewriteMutator(Recipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public Set<String> getTags() {
		return Set.of("OpenRewrite");
	}

	@Override
	public Optional<Result> walkAst(SourceFile pre) {
		AtomicReference<Throwable> refFirstError = new AtomicReference<>();

		ExecutionContext ctx = new InMemoryExecutionContext(t -> {
			if (refFirstError.compareAndSet(null, t)) {
				LOGGER.debug("We register the first exception", t);
			} else {
				LOGGER.warn("Multiple exception are being thrown. This one is being discarded", t);
			}
		});

		RecipeRun run = recipe.run(new InMemoryLargeSourceSet(Arrays.asList(pre)), ctx);
		List<Result> results = run.getChangeset().getAllResults();
		if (results.isEmpty()) {
			return Optional.empty();
		} else if (refFirstError.get() != null) {
			LOGGER.warn("OpenRewrite encountered an error with given AST", refFirstError.get());
			return Optional.empty();
		} else {
			Result result = Iterables.getOnlyElement(results);
			return Optional.of(result);
		}
	}

}
