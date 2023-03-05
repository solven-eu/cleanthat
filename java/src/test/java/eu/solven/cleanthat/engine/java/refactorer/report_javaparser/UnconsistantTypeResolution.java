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

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class UnconsistantTypeResolution {
	static final String testCase = "import java.util.List;\n" + "import java.util.stream.Collectors;\n"
			+ "\n"
			+ "import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;\n"
			+ "\n"
			+ "public class Case_UnclearGenericBounds {\n"
			+ "\n"
			+ "    public List<Class<? extends IMutator>> pre(List<String> classNames) {\n"
			+ "        List<Class<? extends IMutator>> classes = classNames.stream().map(s -> {\n"
			+ "            try {\n"
			+ "                return Class.forName(s);\n"
			+ "            } catch (ClassNotFoundException e) {\n"
			+ "                return null;\n"
			+ "            }\n"
			+ "        }).map(c -> (Class<? extends IMutator>) c.asSubclass(IMutator.class)).collect(Collectors.toList());\n"
			+ "        return classes;\n"
			+ "    }\n"
			+ "}";

	public static void main(String[] args) {
		System.out.println(testCase);

		StaticJavaParser.setConfiguration(
				new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(true))));
		var unit = StaticJavaParser.parse(testCase);

		Expression expr = unit.findFirst(MethodCallExpr.class).get();

		try {
			var type = expr.getSymbolResolver().calculateType(expr);
			System.out.println(type);
		} catch (RuntimeException e) {
			e.printStackTrace();

			var typeSecondTry = expr.getSymbolResolver().calculateType(expr);
			System.out.println(typeSecondTry);
		}

	}
}
