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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class ReportResolvePrimitiveCast {
	static final String testCase = "public class Division_fromShort {\n" + "\n"
			+ "	public float pre(short s1, short s2) {\n"
			+ "		return s1 / s2;\n"
			+ "	}\n"
			+ "\n"
			+ "	public float post(short s1, short s2) {\n"
			+ "		return (float) s1 / s2;\n"
			+ "	}\n"
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

		var castExpr = node.findAll(CastExpr.class).get(0);

		System.out.println(castExpr.calculateResolvedType());
	}
}
