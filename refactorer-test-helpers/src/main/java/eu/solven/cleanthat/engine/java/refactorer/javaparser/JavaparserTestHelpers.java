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
package eu.solven.cleanthat.engine.java.refactorer.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * Core helpers for tests relating with JavaParser
 * 
 * @author Benoit Lacelle
 *
 */
public class JavaparserTestHelpers {
	protected JavaparserTestHelpers() {
		// hidden
	}

	// Duplicated from JavaRefactorer
	public static JavaParser makeDefaultJavaParser(boolean jreOnly) {
		return makeDefaultJavaParser(jreOnly, LanguageLevel.BLEEDING_EDGE);
	}

	public static JavaParser makeDefaultJavaParser(boolean jreOnly, LanguageLevel languageLevel) {
		var reflectionTypeSolver = makeDefaultTypeSolver(jreOnly);

		var symbolResolver = new JavaSymbolSolver(reflectionTypeSolver);

		var configuration = new ParserConfiguration().setSymbolResolver(symbolResolver).setLanguageLevel(languageLevel);
		var parser = new JavaParser(configuration);
		return parser;
	}

	// Duplicated from JavaRefactorer
	public static ReflectionTypeSolver makeDefaultTypeSolver(boolean jreOnly) {
		var reflectionTypeSolver = new ReflectionTypeSolver(jreOnly);
		return reflectionTypeSolver;
	}
}
