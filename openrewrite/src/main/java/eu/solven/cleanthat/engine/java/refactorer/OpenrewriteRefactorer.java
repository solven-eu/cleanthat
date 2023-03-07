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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser.Input;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.formatter.PathAndContent;

/**
 * A {@link IMutator} configuring over an OpenRewrite {@link Recipe}
 * 
 * @author Benoit Lacelle
 *
 */
public class OpenrewriteRefactorer extends AAstRefactorer<J.CompilationUnit, JavaParser, Result, OpenrewriteMutator> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenrewriteRefactorer.class);

	// Is this threadsafe/stateless?
	final ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

	public OpenrewriteRefactorer(List<OpenrewriteMutator> mutators) {
		super(mutators);
	}

	@Override
	public String doFormat(PathAndContent pathAndContent) throws IOException {
		return applyTransformers(pathAndContent);
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
	protected Optional<CompilationUnit> parseSourceCode(JavaParser javaParser, String sourceCode) {
		Input input = Input.fromString(sourceCode, StandardCharsets.UTF_8);

		AtomicReference<Throwable> refFirstError = new AtomicReference<>();

		ExecutionContext ctx = new InMemoryExecutionContext(t -> {
			if (refFirstError.compareAndSet(null, t)) {
				LOGGER.debug("We register the first exception", t);
			} else {
				LOGGER.warn("Multiple exception are being thrown. This one is being discarded", t);
			}
		});

		Path relativeTo = null;

		// TODO Unclear if we could apply the visitor right away
		// see org.openrewrite.Recipe.getVisitor()
		List<J.CompilationUnit> cus = javaParser.parseInputs(Collections.singleton(input), relativeTo, ctx);

		if (refFirstError.get() != null) {
			LOGGER.warn("Issue while parsing the input", refFirstError.get());
			return Optional.empty();
		} else if (cus.isEmpty()) {
			return Optional.empty();
		} else {
			CompilationUnit result = Iterables.getOnlyElement(cus);
			return Optional.of(result);
		}
	}

	@Override
	protected String toString(Result result) {
		return result.getAfter().printAll();
	}

	@Override
	protected boolean isValidResultString(JavaParser parser, String resultAsString) {
		return parseSourceCode(parser, resultAsString).isPresent();
	}

}
