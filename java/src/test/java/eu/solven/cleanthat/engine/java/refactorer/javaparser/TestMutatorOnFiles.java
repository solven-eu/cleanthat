/*
 * Copyright 2023-2024 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.engine.java.refactorer.javaparser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.IDisabledMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LocalVariableTypeInference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.scanner.MutatorsScanner;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserTestCases;

/**
 * This is some sort of integration tests. it will process any resource in `/source/do_not_format_me/XXX` where XXX is
 * an IMutator `.getSimpleName()`, and ensure no error is encountered.
 * 
 * @author Benoit Lacelle
 *
 */
@RunWith(Parameterized.class)
public class TestMutatorOnFiles extends AJavaparserTestCases {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestMutatorOnFiles.class);

	private static final String DIR = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/source/do_not_format_me/";

	protected static Collection<Object[]> listCases() throws IOException {
		ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
		var resources = patternResolver.getResources(DIR + "*");

		List<Object[]> individualCases = new ArrayList<>();

		Stream.of(resources).forEach(mutatorCasesFolder -> {
			var mutatorClassSimpleName = mutatorCasesFolder.getFilename();
			Resource[] cases;
			try {
				cases = patternResolver.getResources(DIR + mutatorClassSimpleName + "/*");
			} catch (IOException e) {
				throw new UncheckedIOException("Issue with " + mutatorCasesFolder, e);
			}

			Class<? extends IMutator> mutatorClass;
			try {
				mutatorClass = (Class<? extends IMutator>) Class
						.forName("eu.solven.cleanthat.engine.java.refactorer.mutators" + "." + mutatorClassSimpleName);
			} catch (ClassNotFoundException e) {
				LOGGER.warn("There is no mutator named: {}", mutatorClassSimpleName);
				return;
			}
			IMutator mutator = MutatorsScanner.instantiate(JavaVersion.parse(IJdkVersionConstants.LAST), mutatorClass);

			JavaParser javaParser = JavaparserTestHelpers.makeDefaultJavaParser(mutator.isJreOnly());

			Stream.of(cases)
					.forEach(oneTestFile -> individualCases.add(new Object[] { mutatorClassSimpleName,
							oneTestFile.getFilename(),
							oneTestFile,
							javaParser,
							mutator }));
		});

		return individualCases;
	}

	// https://github.com/junit-team/junit4/wiki/parameterized-tests
	@Parameters(name = "{0} - {1}")
	public static Collection<Object[]> data() throws IOException {
		return listCases();
	}

	final Resource resource;
	final JavaParser javaParser;
	final IWalkingMutator<Node, Node> mutator;

	public TestMutatorOnFiles(String mutatorSimplename,
			String fileName,
			Resource resource,
			JavaParser javaParser,
			IWalkingMutator<Node, Node> mutator) {
		this.resource = resource;
		this.javaParser = javaParser;
		this.mutator = mutator;
	}

	private void testSourceWithMutator(IWalkingMutator<Node, Node> mutator, String asString) {
		if ("SealedClassTests.java".equals(resource.getFilename())) {
			// sealed classes are not managed by JP3.25
			Assertions.assertThatThrownBy(() -> parseCompilationUnit(mutator, asString))
					.isInstanceOf(IllegalArgumentException.class);
		} else if ("AsyncLoggerConfig.java".equals(resource.getFilename())) {
			if (mutator.getClass().equals(LocalVariableTypeInference.class)) {
				// https://github.com/javaparser/javaparser/issues/3940
				Assertions.assertThatThrownBy(() -> {
					var compilationUnit = parseCompilationUnit(mutator, asString);
					mutator.walkAstHasChanged(compilationUnit);
				}).isInstanceOf(StackOverflowError.class);
			} else {
				LOGGER.debug("{} does often a {}. but not always", resource, StackOverflowError.class);
			}
		} else {
			CompilationUnit ast = parseCompilationUnit(mutator, asString);

			Optional<Node> result = mutator.walkAst(ast);

			if (result.isPresent()) {
				var resultAsString = LexicalPreservingPrinter.print(result.get());
				// Try parsing the result, to check we produced valid code
				parseCompilationUnit(mutator, resultAsString);
			}
		}
	}

	@Test
	public void testWithDedicatedMutator() throws IOException {
		LOGGER.info("Processing: {}", resource);

		if ("Issue807.java".equals(resource.getFilename())) {
			LOGGER.warn("We skip {} because JavaParser produces an invalid Java file", resource.getFilename());
			return;
		}

		var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);
		testSourceWithMutator(mutator, asString);
	}

	// Resources are focus for a given mutator: we can still check they do not break other mutators
	@Test
	public void testWithAllMutators() throws IOException {
		LOGGER.info("Processing: {}", resource);

		if ("Issue807.java".equals(resource.getFilename())) {
			LOGGER.warn("We skip {} because JavaParser produces an invalid Java file", resource.getFilename());
			return;
		}

		var asString = new String(ByteStreams.toByteArray(resource.getInputStream()), StandardCharsets.UTF_8);

		AllIncludingDraftSingleMutators composite =
				new AllIncludingDraftSingleMutators(JavaVersion.parse(IJdkVersionConstants.LAST));
		for (IMutator anyMutator : composite.getUnderlyings()) {
			if (anyMutator instanceof IDisabledMutator) {
				// These mutators are not ready
				continue;
			}

			IWalkingMutator<Node, Node> mutator = (IWalkingMutator<Node, Node>) anyMutator;

			testSourceWithMutator(mutator, asString);
		}
	}
}
