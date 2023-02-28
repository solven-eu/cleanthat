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

import org.assertj.core.api.Assertions;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class ReportTextBlockStripIndent {
	static final String testCase = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
			+ "import java.util.Optional;\n"
			+ "\n"
			+ "public class SomeClass {\n"
			+ "\n"
			+ "	String html = \"\"\"\n"
			+ "			<html>\n"
			+ "				<head>\n"
			+ "					<meta charset=\"utf-8\">\n"
			+ "				</head>\n"
			+ "				<body class=\"default-view\" style=\"word-wrap: break-word;\">\n"
			+ "					<p>Hello, world</p>\n"
			+ "				</body>\n"
			+ "			</html>\n"
			+ "			\"\"\";\n"
			+ "}";

	public static ReflectionTypeSolver makeDefaultTypeSolver(boolean jreOnly) {
		var reflectionTypeSolver = new ReflectionTypeSolver(jreOnly);
		return reflectionTypeSolver;
	}

	public static JavaParser makeDefaultJavaParser(boolean jreOnly) {
		var reflectionTypeSolver = makeDefaultTypeSolver(jreOnly);

		var symbolResolver = new JavaSymbolSolver(reflectionTypeSolver);

		var configuration = new ParserConfiguration().setSymbolResolver(symbolResolver);
		var parser = new JavaParser(configuration);
		return parser;
	}

	public static void main(String[] args) {
		var node = makeDefaultJavaParser(true).parse(testCase).getResult().get();

		node = LexicalPreservingPrinter.setup(node);

		var textBlock = node.findAll(TextBlockLiteralExpr.class).get(0);

		Assertions.assertThat(textBlock.getValue()).startsWith("\t\t\t<html>");

		var newExprFromStrippedString = new TextBlockLiteralExpr(textBlock.stripIndent());

		Assertions.assertThat(textBlock.toString()).isEqualTo(newExprFromStrippedString.toString());
		Assertions.assertThat(textBlock).isEqualTo(newExprFromStrippedString);
	}
}
