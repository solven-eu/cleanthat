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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

public class LexicalPreservingTextBlocks {
	static final String testCase = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
			+ "import java.util.Optional;\n"
			+ "\n"
			+ "public class SomeClass {\n"
			+ "\n"
			+ "	String html = \"\" + \"<html>\\n\"\n"
			+ "			+ \"\\t<head>\\n\"\n"
			+ "			+ \"\\t\\t<meta charset=\\\"utf-8\\\">\\n\"\n"
			+ "			+ \"\\t</head>\\n\"\n"
			+ "			+ \"\\t<body class=\\\"default-view\\\" style=\\\"word-wrap: break-word;\\\">\\n\"\n"
			+ "			+ \"\\t\\t<p>Hello, world</p>\\n\"\n"
			+ "			+ \"\\t</body>\\n\"\n"
			+ "			+ \"</html>\\n\";\n"
			+ "}";

	static final String newText = "			<html>\n" + "				<head>\n"
			+ "					<meta charset=\"utf-8\">\n"
			+ "				</head>\n"
			+ "				<body class=\"default-view\" style=\"word-wrap: break-word;\">\n"
			+ "					<p>Hello, world</p>\n"
			+ "				</body>\n"
			+ "			</html>";

	public static void main(String[] args) {
		var unit = StaticJavaParser.parse(testCase);

		unit = LexicalPreservingPrinter.setup(unit);

		var expr = unit.findFirst(VariableDeclarator.class).get();

		expr.setInitializer(new TextBlockLiteralExpr(newText));

		System.out.println("KO");
		System.out.println(LexicalPreservingPrinter.print(expr));

		System.out.println();

		System.out.println("OK");
		System.out.println(expr.toString());
	}
}
