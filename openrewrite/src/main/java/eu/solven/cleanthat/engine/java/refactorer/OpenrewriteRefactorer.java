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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser.Input;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import com.google.common.collect.Iterables;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.formatter.LineEnding;

/**
 * A {@link IMutator} configuring over an OpenRewrite {@link Recipe}
 * 
 * @author Benoit Lacelle
 *
 */
public class OpenrewriteRefactorer extends AAstRefactorer<J.CompilationUnit, JavaParser, Result, OpenrewriteMutator> {

	// Is this threadsafe/stateless?
	final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

	public OpenrewriteRefactorer(List<OpenrewriteMutator> mutators) {
		super(mutators);
	}

	@Override
	public String doFormat(String content, LineEnding ending) throws IOException {
		return applyTransformers(content);
	}

	@Override
	public String getId() {
		return "openrewrite";
	}

	@Override
	protected JavaParser makeAstParser() {
		// determine your project directory and provide a list of
		// paths to jars that represent the project's classpath
		// Path projectDir = Paths.get(".");
		List<Path> classpath = Collections.emptyList();

		// create a JavaParser instance with your classpath
		return JavaParser.fromJavaVersion().classpath(classpath).build();
	}

	@Override
	protected J.CompilationUnit parseSourceCode(JavaParser javaParser, String sourceCode) {
		ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

		// parser the source files into LSTs
		// Beware Path implements Iterable<Path>
		Path relativeTo = null;
		Input input = Input.fromString(sourceCode, StandardCharsets.UTF_8);

		// TODO Unclear if we could apply the visitor right away
		// see org.openrewrite.Recipe.getVisitor()
		List<J.CompilationUnit> cus = javaParser.parseInputs(Collections.singleton(input), relativeTo, ctx);

		return Iterables.getOnlyElement(cus);
	}

	@Override
	protected String toString(Result result) {
		return result.getAfter().printAll();
	}

}
