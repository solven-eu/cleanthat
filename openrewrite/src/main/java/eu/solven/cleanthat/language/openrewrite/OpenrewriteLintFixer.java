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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.ILintFixerWithId;
import eu.solven.cleanthat.formatter.ILintFixerWithPath;
import eu.solven.cleanthat.formatter.LineEnding;
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
	public String doFormat(PathAndContent pathAndContent, LineEnding ending) throws IOException {
		Path path = pathAndContent.getPath();

		Files.createDirectories(path.getParent());
		Files.writeString(path, pathAndContent.getContent());

		Path root = CodeProviderHelpers.getRoot(path);

		// determine your project directory and provide a list of
		// paths to jars that represent the project's classpath
		// Path projectDir = Paths.get(".");
		List<Path> classpath = Collections.emptyList();

		// create a JavaParser instance with your classpath
		JavaParser javaParser = JavaParser.fromJavaVersion().classpath(classpath).build();

		ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

		// parser the source files into LSTs
		// Beware Path implements Iterable<Path>
		List<J.CompilationUnit> cus = javaParser.parse(Collections.singleton(path), root, ctx);

		// collect results
		List<Result> results = recipe.run(cus, ctx).getResults();

		if (results.size() != 1) {
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

	@Override
	public String doFormat(String content, LineEnding ending) throws IOException {
		return doFormat(new PathAndContent(null, content), ending);
	}

}
