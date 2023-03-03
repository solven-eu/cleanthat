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

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

@RunWith(Parameterized.class)
public abstract class AParameterizesJavaparserRefactorerCases extends AParameterizesRefactorerCases<Node, Node> {

	public AParameterizesJavaparserRefactorerCases(JavaParser javaParser, ClassOrInterfaceDeclaration testCase) {
		super(javaParser, testCase);
	}

	protected abstract ARefactorerCases<Node, Node, IJavaparserMutator> getCases();

	@Override
	protected Node convertToAst(Node node) {
		// Many issues are specific to LexicalPreservingPrinter.setup
		// https://github.com/javaparser/javaparser/issues/3898
		// https://github.com/javaparser/javaparser/issues/3924
		LexicalPreservingPrinter.setup(node);

		return node;
	}

	@Override
	protected String astToString(Node node) {
		return LexicalPreservingPrinter.print(node);
		// return node.toString();
	}

	@Override
	protected String resultToString(Node node) {
		return LexicalPreservingPrinter.print(node);
		// return node.toString();
	}
}
