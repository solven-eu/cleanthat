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
package eu.solven.cleanthat.openrewrite.cases;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;
import org.openrewrite.Result;
import org.openrewrite.java.tree.J;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import eu.solven.cleanthat.engine.java.refactorer.AAstRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteMutator;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.cases.AParameterizesRefactorerCases;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;
import eu.solven.cleanthat.openrewrite.cases.do_not_format_me.CommonStaticAnalysisCases;

@Ignore("TODO")
public class TestCommonStaticAnalysisCases extends AParameterizesRefactorerCases<J.CompilationUnit, Result> {

	final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());

	private static ARefactorerCases<J.CompilationUnit, Result, OpenrewriteMutator> getStaticRefactorerCases() {
		return new CommonStaticAnalysisCases();
	}

	public TestCommonStaticAnalysisCases(JavaParser javaParser, String testName, ClassOrInterfaceDeclaration testCase) {
		super(javaParser, testCase);
	}

	// https://github.com/junit-team/junit4/wiki/parameterized-tests
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() throws IOException {
		ARefactorerCases<J.CompilationUnit, Result, OpenrewriteMutator> testCases = getStaticRefactorerCases();
		return listCases(testCases);
	}

	@Override
	protected ARefactorerCases<J.CompilationUnit, Result, OpenrewriteMutator> getCases() {
		return getStaticRefactorerCases();
	}

	@Override
	protected J.CompilationUnit convertToAst(Node pre) {
		var asString = pre.toString();

		return AAstRefactorer.parse(refactorer, asString);
	}

	@Override
	protected <T extends Node> String toString(T post) {
		return post.toString();
	}

	@Override
	protected String toString(Result post) {
		return post.getAfter().printAll();
	}
}
