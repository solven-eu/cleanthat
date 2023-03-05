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
package eu.solven.cleanthat.engine.java.refactorer.test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;

/**
 * {@link ATestCases}for {@link IJavaparserMutator}
 * 
 * @author Benoit Lacelle
 *
 */
public class AJavaparserTestCases extends ATestCases<Node, Node> {

	@Override
	protected Node convertToAst(Node ast) {
		LexicalPreservingPrinter.setup(ast);

		return ast;
	}

	@Override
	protected String astToString(Node node) {
		return LexicalPreservingPrinter.print(node);
		// return post.toString();
	}

	@Override
	protected String resultToString(Node node) {
		return LexicalPreservingPrinter.print(node);
		// return post.toString();
	}

	protected CompilationUnit parseCompilationUnit(IMutator mutator, String asString) {
		var javaParser = JavaRefactorer.makeDefaultJavaParser(mutator.isJreOnly());
		var compilationUnit = ATestCases.throwIfProblems(javaParser.parse(asString));
		LexicalPreservingPrinter.setup(compilationUnit);
		return compilationUnit;
	}
}
