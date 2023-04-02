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
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.collect.ImmutableMap;

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
	public static TypeSolver makeDefaultTypeSolver(boolean jreOnly) {
		var reflectionTypeSolver = new ReflectionTypeSolver(jreOnly);

		MemoryTypeSolver memoryTypeSolver = new MemoryTypeSolver();
		var guavaImmutableMap = new ReflectionClassDeclaration(ImmutableMap.class, reflectionTypeSolver);

		// This is typically used by GuavaImmutableMapBuilderOverVarargs
		// This mechanism needs to be made generic
		// As it stands, it means the source input will be present by the dependency in the version used by CleanThat at
		// runtime: this is a bad thing as the sourceCode may rely on a completely different version through its
		// build-system. However, Cleanthat targets not to load the whole target dependencies
		memoryTypeSolver.addDeclaration(ImmutableMap.class.getName(), guavaImmutableMap);

		return new CombinedTypeSolver(reflectionTypeSolver, memoryTypeSolver);
	}
}
