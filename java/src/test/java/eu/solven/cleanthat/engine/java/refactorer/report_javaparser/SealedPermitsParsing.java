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
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

// https://github.com/javaparser/javaparser/issues/4245
// https://github.com/solven-eu/cleanthat/issues/713
public class SealedPermitsParsing {
	static final String testCase =
			"public sealed interface IUpdatePortCommand permits UpdateScheduleCommand, UpdateStateCommand {}";

	public static void main(String[] args) {
		System.out.println(testCase);

		ParserConfiguration parserConfiguration = new ParserConfiguration();
		parserConfiguration.setLanguageLevel(LanguageLevel.JAVA_17);
		StaticJavaParser.setConfiguration(
				parserConfiguration.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(true))));
		var unit = StaticJavaParser.parse(testCase);

		unit = LexicalPreservingPrinter.setup(unit);

		ClassOrInterfaceDeclaration classOrInterface = unit.findFirst(ClassOrInterfaceDeclaration.class).get();
		classOrInterface.setModifiers();

		System.out.println(classOrInterface);
	}
}
