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

import java.util.List;
import java.util.Map;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class UnconsistantTypeResolution_ImmutableMapOf {
	static final String testCase = "import java.util.Map;\n" + "\n"
			+ "public class GuavaImmutableMapBuilderInsteadOfVarargs{\n"
			+ "	public Object pre() {\n"
			+ "		return Map.of(\"k0\", 0, \"k1\", 1D);\n"
			+ "	}\n"
			+ "}\n"
			+ "";

	public static void main(String[] args) {
		System.out.println(testCase);

		ReflectionTypeSolver jreTypeSolver = new ReflectionTypeSolver(true);

		StaticJavaParser
				.setConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(jreTypeSolver)));
		var unit = StaticJavaParser.parse(testCase);

		MethodCallExpr methodCallExpr = unit.findFirst(MethodCallExpr.class).get();

		JavaParserFacade jpf = JavaParserFacade.get(jreTypeSolver);
		MethodUsage methodUsage = jpf.solveMethodAsUsage(methodCallExpr);

		List<ResolvedType> types = methodUsage.getParamTypes();
		System.out.println(types.get(0));
		System.out.println(types.get(1));
		System.out.println(types.get(2));
		System.out.println(types.get(3));

		ResolvedTypeParametersMap parametersMap = methodUsage.typeParametersMap();
		System.out.println(parametersMap.getTypes());
	}

	public Map<String, Number> pre() {
		return Map.of("k0", 0, "k1", 1D);
	}

}
