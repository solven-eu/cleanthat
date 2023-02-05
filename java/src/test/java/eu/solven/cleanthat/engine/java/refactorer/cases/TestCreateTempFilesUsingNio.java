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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.CreateTempFilesUsingNioCases;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.CreateTempFilesUsingNio;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCreateTempFilesUsingNio extends AParameterizesRefactorerCases {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCreateTempFilesUsingNio.class);

	private static ARefactorerCases getStaticRefactorerCases() {
		return new CreateTempFilesUsingNioCases();
	}

	public TestCreateTempFilesUsingNio(JavaParser javaParser, String testName, ClassOrInterfaceDeclaration testCase) {
		super(javaParser, testName, testCase);
	}

	// https://github.com/junit-team/junit4/wiki/parameterized-tests
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() throws IOException {
		ARefactorerCases testCases = getStaticRefactorerCases();
		return listCases(testCases);
	}

	@Override
	protected ARefactorerCases getCases() {
		return getStaticRefactorerCases();
	}

	@Test
	public void testImportNioFiles() throws IOException {
		JavaParser javaParser =
				JavaRefactorer.makeDefaultJavaParser(getStaticRefactorerCases().getTransformer().isJreOnly());
		CompilationUnit compilationUnit =
				javaParser.parse(LocalClassTestHelper.localClassAsPath(CreateTempFilesUsingNioCases.class))
						.getResult()
						.get();

		List<ClassOrInterfaceDeclaration> cases = compilationUnit.findAll(ClassOrInterfaceDeclaration.class, c -> {
			return !c.getMethodsByName("pre").isEmpty() && !c.getMethodsByName("post").isEmpty();
		});
		IMutator transformer = new CreateTempFilesUsingNio();
		cases.forEach(oneCase -> {
			LOGGER.info("Processing the case: {}", oneCase.getName());
			MethodDeclaration pre = getMethodWithName(oneCase, "pre");
			MethodDeclaration post = getMethodWithName(oneCase, "post");
			// Check if 'pre' transformation into 'post' add the good library
			// TODO not yet testing import of Paths
			{
				compilationUnit.getImports()
						.removeIf(im -> new ImportDeclaration("java.nio.file.Files", false, false).equals(im));
				transformer.walkNode(pre);
				// Rename the method before checking full equality
				pre.setName("post");
				Assert.assertTrue(compilationUnit.getImports()
						.contains(new ImportDeclaration("java.nio.file.Files", false, false)));
			}
		});
	}
}
