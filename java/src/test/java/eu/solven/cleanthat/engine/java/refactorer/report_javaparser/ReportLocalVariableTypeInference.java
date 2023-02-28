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
package eu.solven.cleanthat.engine.java.refactorer.report_javaparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

public class ReportLocalVariableTypeInference {
	static final String testCase = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
			+ "import java.util.HashMap;\n"
			+ "import java.util.Map;\n"
			+ "public class LocalVariableTypeInferenceCases {\n"
			+ "	public Object pre() {\n"
			+ "			Map<String, ?> i = new HashMap<>();\n"
			+ "			return i;"
			+ "	}\n"
			+ "}\n"
			+ "";

	static boolean processNotRecursively(Node node) {
		if (!(node instanceof VariableDeclarationExpr)) {
			return false;
		}
		var variableDeclarationExpr = (VariableDeclarationExpr) node;

		if (variableDeclarationExpr.getVariables().size() >= 2) {
			return false;
		}

		var singleVariableDeclaration = variableDeclarationExpr.getVariable(0);

		if (singleVariableDeclaration.getType().isVarType()) {
			return false;
		}

		singleVariableDeclaration.setType(new VarType());

		return true;
	}

	public static void main(String[] args) {
		var node = StaticJavaParser.parse(testCase);

		node = LexicalPreservingPrinter.setup(node);

		node.walk(innerNode -> {
			processNotRecursively(innerNode);
		});
	}
}
