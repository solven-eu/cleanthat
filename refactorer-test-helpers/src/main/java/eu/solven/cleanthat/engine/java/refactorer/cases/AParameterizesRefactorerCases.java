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
package eu.solven.cleanthat.engine.java.refactorer.cases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethodsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;
import eu.solven.cleanthat.engine.java.refactorer.test.ATestCases;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;

/**
 * Base class enabling each testCase to appear as an individual JUnit testCase
 * 
 * @author Benoit Lacelle
 *
 * @param <AST>
 * @param <R>
 */
@RunWith(Parameterized.class)
public abstract class AParameterizesRefactorerCases<AST, R> extends ATestCases<AST, R> {

	final JavaParser javaParser;

	final ClassOrInterfaceDeclaration testCase;

	// Duplicated from JavaRefactorer
	public static JavaParser makeDefaultJavaParser(boolean jreOnly) {
		ReflectionTypeSolver reflectionTypeSolver = makeDefaultTypeSolver(jreOnly);

		JavaSymbolSolver symbolResolver = new JavaSymbolSolver(reflectionTypeSolver);

		ParserConfiguration configuration = new ParserConfiguration().setSymbolResolver(symbolResolver);
		JavaParser parser = new JavaParser(configuration);
		return parser;
	}

	// Duplicated from JavaRefactorer
	public static ReflectionTypeSolver makeDefaultTypeSolver(boolean jreOnly) {
		ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver(jreOnly);
		return reflectionTypeSolver;
	}

	protected static Collection<Object[]> listCases(ARefactorerCases<?, ?, ?> testCases) throws IOException {
		String path = LocalClassTestHelper.loadClassAsString(testCases.getClass());

		JavaParser javaParser = makeDefaultJavaParser(testCases.getTransformer().isJreOnly());
		CompilationUnit compilationUnit = javaParser.parse(path).getResult().get();

		List<Object[]> individualCases = new ArrayList<>();

		getAllCases(compilationUnit).stream()
				.map(t -> new Object[] { javaParser, t.getName().toString(), t })
				.forEach(individualCases::add);

		return individualCases;
	}

	public AParameterizesRefactorerCases(JavaParser javaParser, String testName, ClassOrInterfaceDeclaration testCase) {
		this.javaParser = javaParser;
		this.testCase = testCase;
	}

	@Test
	public void testCase() {
		Assume.assumeFalse("Ignored", testCase.getAnnotationByClass(Ignore.class).isPresent());

		ARefactorerCases<AST, R, ? extends IWalkingMutator<AST, R>> cases = getCases();
		IWalkingMutator<AST, R> transformer = cases.getTransformer();

		if (testCase.getAnnotationByClass(CompareMethods.class).isPresent()) {
			doTestMethod(transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareTypes.class).isPresent()) {
			doCompareTypes(transformer, testCase);
		}

		if (testCase.getAnnotationByClass(UnmodifiedMethod.class).isPresent()) {
			doCheckUnmodified(transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareClasses.class).isPresent()) {
			doCompareClasses(javaParser, transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareInnerClasses.class).isPresent()) {
			doCompareInnerClasses(javaParser, transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareInnerAnnotations.class).isPresent()) {
			doCompareInnerAnnotations(javaParser, transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareMethodsAsStrings.class).isPresent()) {
			doCompareMethodsAsStrings(javaParser, transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareCompilationUnitsAsStrings.class).isPresent()) {
			ResolvedReferenceTypeDeclaration resolved = testCase.resolve();
			// JavaParser does not print the '$' of nested qualified classname
			// https://github.com/javaparser/javaparser/issues/1518
			String qualifiedClassName =
					resolved.getPackageName() + "." + resolved.getClassName().replaceFirst("\\.", "\\$");
			Class<?> realTestCase;
			try {
				realTestCase = Class.forName(qualifiedClassName);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Issue with " + qualifiedClassName, e);
			}
			// This is useful to get fully resolved annotations (e.g. String concatenations)
			CompareCompilationUnitsAsStrings realAnnotation =
					realTestCase.getAnnotationsByType(CompareCompilationUnitsAsStrings.class)[0];
			doCompareCompilationUnitsAsStrings(javaParser, transformer, testCase, realAnnotation);
		}

		if (testCase.getAnnotationByClass(UnmodifiedCompilationUnitAsString.class).isPresent()) {
			ResolvedReferenceTypeDeclaration resolved = testCase.resolve();
			// JavaParser does not print the '$' of nested qualified classname
			// https://github.com/javaparser/javaparser/issues/1518
			String qualifiedClassName =
					resolved.getPackageName() + "." + resolved.getClassName().replaceFirst("\\.", "\\$");
			Class<?> realTestCase;
			try {
				realTestCase = Class.forName(qualifiedClassName);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Issue with " + qualifiedClassName, e);
			}
			// This is useful to get fully resolved annotations (e.g. String concatenations)
			UnmodifiedCompilationUnitAsString realAnnotation =
					realTestCase.getAnnotationsByType(UnmodifiedCompilationUnitAsString.class)[0];
			doCheckUnmodifiedCompilationUnitsAsStrings(javaParser, transformer, testCase, realAnnotation);
		}
	}

	protected abstract ARefactorerCases<AST, R, ? extends IWalkingMutator<AST, R>> getCases();
}
