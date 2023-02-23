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
package eu.solven.cleanthat.language.openrewrite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser.Input;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.ILintFixerWithPath;
import eu.solven.cleanthat.formatter.PathAndContent;

/**
 * {@link ILintFixer} for OpenRewrite engine. See https://docs.openrewrite.org/
 * 
 * @author Benoit Lacelle
 *
 */
// https://docs.openrewrite.org/running-recipes/running-rewrite-without-build-tool-plugins
public class OpenrewriteLintFixer implements ILintFixerWithId, ILintFixerWithPath {

	final Recipe recipe;

	/**
	 * 
	 * @param recipe
	 *            the recipe to apply. May be a {@link CompositeRecipe}
	 */
	public OpenrewriteLintFixer(Recipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public String doFormat(PathAndContent pathAndContent) throws IOException {
		Input input = Input.fromString(pathAndContent.getPath(), pathAndContent.getContent());
		return formatInput(input);
	}

	@Override
	public String doFormat(String content) throws IOException {
		Input input = Input.fromString(content);
		return formatInput(input);
	}

	protected String formatInput(Input input) {
		// paths to jars that represent the project's classpath
		// Path projectDir = Paths.get(".");
		List<Path> classpath = Collections.emptyList();

		// create a JavaParser instance with your classpath
		JavaParser javaParser = JavaParser.fromJavaVersion().classpath(classpath).build();

		ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

		// parser the source files into LSTs
		List<J.CompilationUnit> cus = javaParser.parseInputs(Collections.singleton(input), null, ctx);

		// collect results
		List<Result> results = recipe.run(cus, ctx).getResults();

		if (results.isEmpty()) {
			// no change
			return input.getSource(ctx).readFully();
		} else if (results.size() != 1) {
			throw new IllegalStateException("We expected a single result in return. Got: " + results.size());
		}

		return results.get(0).getAfter().printAll();
	}

	@Override
	public String getId() {
		return "openrewrite";
	}

	@Override
	public String toString() {
		return "Recipe: " + recipe.toString();
	}

}
