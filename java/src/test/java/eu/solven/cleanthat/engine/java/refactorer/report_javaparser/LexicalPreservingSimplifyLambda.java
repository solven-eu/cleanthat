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
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

public class LexicalPreservingSimplifyLambda {
	static final String testCase = "package eu.solven.cleanthat.code_provider.inmemory;\n" + "\n"
			+ "import java.util.stream.Stream;\n"
			+ "\n"
			+ "public class TestFileSystemCodeProvider {\n"
			+ "	@Test\n"
			+ "	public void testInMemoryFileSystem() throws IOException {\n"
			+ "\n"
			+ "		Stream.of(\"\").listFilesForContent(file -> {\n"
			+ "			System.out.println(s);\n"
			+ "		});\n"
			+ "	}\n"
			+ "}\n"
			+ "";

	public static void main(String[] args) {
		var unit = StaticJavaParser.parse(testCase);

		unit = LexicalPreservingPrinter.setup(unit);

		unit.walk(node -> {
			if (node instanceof LambdaExpr) {
				var lambdaExpr = (LambdaExpr) node;

				var body = lambdaExpr.getBody();

				if (!(body instanceof BlockStmt)) {
					return;
				}

				var lambdaBLockStmt = (BlockStmt) body;

				if (lambdaBLockStmt.getStatements().size() == 1) {
					var exprStmt = (ExpressionStmt) lambdaBLockStmt.getStatement(0);

					lambdaExpr.setBody(new ExpressionStmt(exprStmt.getExpression()));
				}
			}
		});

		System.out.println("OK");
		System.out.println(unit.toString());

		System.out.println("KO");
		System.out.println(LexicalPreservingPrinter.print(unit));
	}
}
