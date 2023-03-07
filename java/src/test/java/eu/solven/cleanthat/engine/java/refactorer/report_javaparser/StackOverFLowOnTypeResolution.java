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

public class StackOverFLowOnTypeResolution {
	static final String testCase = "package org.apache.logging.log4j.core.async;\n" + "\n"
			+ "public class AsyncLoggerConfig {\n"
			+ "\n"
			+ "    public static class RootLogger {\n"
			+ "\n"
			+ "        public static class Builder<B extends Builder<B>> extends RootLogger.Builder<B> {\n"
			+ "\n"
			+ "            @Override\n"
			+ "            public LoggerConfig build() {\n"
			+ "                LevelAndRefs container = LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(),\n"
			+ "                        getConfig());\n"
			+ "                return new AsyncLoggerConfig(LogManager.ROOT_LOGGER_NAME);\n"
			+ "            }\n"
			+ "        }\n"
			+ "    }\n"
			+ "}\n"
			+ "";

	public static void main(String[] args) {
		System.out.println(testCase);

		StaticJavaParser.setConfiguration(
				new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(true))));
		var unit = StaticJavaParser.parse(testCase);

		Expression expr = unit.findFirst(MethodCallExpr.class).get();

		expr.getSymbolResolver().calculateType(expr);
	}
}
