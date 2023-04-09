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
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethodsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClass;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.javaparser.JavaparserTestHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.ATestCases;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;
import eu.solven.cleanthat.engine.java.refactorer.test.OneMutatorCase;

/**
 * Base class enabling each testCase to appear as an individual JUnit testCase
 * 
 * @author Benoit Lacelle
 *
 * @param <N>
 * @param <R>
 */
public abstract class AParameterizesRefactorerCases<N, R> extends ATestCases<N, R> {

	public static Stream<Arguments> listCases(AParameterizesRefactorerCases<?, ?> testCases) throws IOException {
		String casesAsString = LocalClassTestHelper.loadClassAsString(testCases.getClass());

		IMutator mutator = testCases.getTransformer();
		JavaParser javaParser = JavaparserTestHelpers.makeDefaultJavaParser(mutator.isJreOnly());
		var compilationUnit = OneMutatorCase.throwIfProblems(javaParser.parse(casesAsString));

		List<Arguments> individualCases = new ArrayList<>();

		getAllCases(compilationUnit).stream()
				.map(oneCase -> Arguments.of(oneCase.getNameAsString().toString(), javaParser, mutator, oneCase))
				.forEach(individualCases::add);

		return individualCases.stream();
	}

	protected abstract IMutator getTransformer();

	@SuppressWarnings("PMD.NPathComplexity")
	@ParameterizedTest(name = "{0}")
	@ArgumentsSource(CasesArgumentsProvider.class)
	void oneTestCase(String testCaseName,
			JavaParser javaParser,
			IWalkingMutator<N, R> mutator,
			ClassOrInterfaceDeclaration testCase) {
		Assume.assumeFalse("Ignored", testCase.getAnnotationByClass(Ignore.class).isPresent());

		// ARefactorerCases<AST, R, ? extends IWalkingMutator<AST, R>> cases = getCases();
		// IWalkingMutator<AST, R> transformer = cases.getTransformer();

		OneMutatorCase<N, R> oneTestCase = new OneMutatorCase<>(javaParser, mutator, this);

		var atLeastOnetest = false;
		if (testCase.getAnnotationByClass(CompareMethods.class).isPresent()) {
			atLeastOnetest |= true;

			if (getNotModifiedUntilImplemented(testCase)) {
				oneTestCase.doCheckUnmodifiedMethod(testCase);
			} else {
				oneTestCase.doTestMethod(testCase);
			}
		} else if (testCase.getAnnotationByClass(UnmodifiedMethod.class).isPresent()) {
			atLeastOnetest |= true;
			oneTestCase.doCheckUnmodifiedMethod(testCase);
		}

		if (testCase.getAnnotationByClass(CompareInnerClasses.class).isPresent()) {
			atLeastOnetest |= true;

			if (getNotModifiedUntilImplemented(testCase)) {
				oneTestCase.doCheckUnmodifiedClass(testCase);
			} else {
				oneTestCase.doCompareInnerClasses(testCase);
			}
		} else if (testCase.getAnnotationByClass(UnmodifiedInnerClass.class).isPresent()) {
			atLeastOnetest |= true;
			oneTestCase.doCheckUnmodifiedClass(testCase);
		}

		if (testCase.getAnnotationByClass(CompareTypes.class).isPresent()) {
			atLeastOnetest |= true;
			oneTestCase.doCompareTypes(testCase);
		}

		if (testCase.getAnnotationByClass(CompareClasses.class).isPresent()) {
			atLeastOnetest |= true;
			oneTestCase.doCompareClasses(testCase);
		}

		if (testCase.getAnnotationByClass(CompareInnerAnnotations.class).isPresent()) {
			atLeastOnetest |= true;
			oneTestCase.doCompareInnerAnnotations(testCase);
		}

		if (testCase.getAnnotationByClass(CompareMethodsAsStrings.class).isPresent()) {
			atLeastOnetest |= true;
			oneTestCase.doCompareMethodsAsStrings(testCase);
		}

		if (testCase.getAnnotationByClass(CompareCompilationUnitsAsStrings.class).isPresent()) {
			atLeastOnetest |= true;
			Class<?> realTestCase = loadTestCaseRealClass(testCase);
			// This is useful to get fully resolved annotations (e.g. String concatenations)
			CompareCompilationUnitsAsStrings realAnnotation =
					realTestCase.getAnnotationsByType(CompareCompilationUnitsAsStrings.class)[0];
			oneTestCase.doCompareCompilationUnitsAsStrings(testCase, realAnnotation);
		} else if (testCase.getAnnotationByClass(UnmodifiedCompilationUnitAsString.class).isPresent()) {
			atLeastOnetest |= true;
			Class<?> realTestCase = loadTestCaseRealClass(testCase);
			// This is useful to get fully resolved annotations (e.g. String concatenations)
			UnmodifiedCompilationUnitAsString realAnnotation =
					realTestCase.getAnnotationsByType(UnmodifiedCompilationUnitAsString.class)[0];
			oneTestCase.doCheckUnmodifiedCompilationUnitsAsStrings(testCase, realAnnotation);
		}

		if (testCase.getAnnotationByClass(UnmodifiedCompilationUnitAsString.class).isPresent()) {
			atLeastOnetest |= true;
			Class<?> realTestCase = loadTestCaseRealClass(testCase);
			// This is useful to get fully resolved annotations (e.g. String concatenations)
			UnmodifiedCompilationUnitAsString realAnnotation =
					realTestCase.getAnnotationsByType(UnmodifiedCompilationUnitAsString.class)[0];
			oneTestCase.doCheckUnmodifiedCompilationUnitsAsStrings(testCase, realAnnotation);
		}

		if (testCase.getAnnotationByClass(CompareCompilationUnitsAsResources.class).isPresent()) {
			atLeastOnetest |= true;
			Class<?> realTestCase = loadTestCaseRealClass(testCase);
			// This is useful to get fully resolved annotations (e.g. String concatenations)
			CompareCompilationUnitsAsResources realAnnotation =
					realTestCase.getAnnotationsByType(CompareCompilationUnitsAsResources.class)[0];
			oneTestCase.doCompareCompilationUnitsAsResources(testCase, realAnnotation);
		}

		Assertions.assertThat(atLeastOnetest).isTrue();
	}

	/**
	 * 
	 * @param testCase
	 * @return true if this case is not implemented yet, and we should ensure the node is actually not modified
	 */
	private boolean getNotModifiedUntilImplemented(ClassOrInterfaceDeclaration testCase) {
		if (testCase.getAnnotationByClass(CaseNotYetImplemented.class).isEmpty()) {
			return false;
		}

		Class<?> realTestCase = loadTestCaseRealClass(testCase);
		// This is useful to get fully resolved annotations (e.g. boolean `unmodifiedUntilImplemented`)
		CaseNotYetImplemented realAnnotation = realTestCase.getAnnotationsByType(CaseNotYetImplemented.class)[0];

		boolean isNotmodifiedUntilImplemented = realAnnotation.unmodifiedUntilImplemented();
		return isNotmodifiedUntilImplemented;
	}

	private Class<?> loadTestCaseRealClass(ClassOrInterfaceDeclaration testCase) {
		var resolved = testCase.resolve();
		// JavaParser does not print the '$' of nested qualified classname
		// https://github.com/javaparser/javaparser/issues/1518
		var qualifiedClassName = resolved.getPackageName() + "." + resolved.getClassName().replaceFirst("\\.", "\\$");
		Class<?> realTestCase;
		try {
			realTestCase = Class.forName(qualifiedClassName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Issue with " + qualifiedClassName, e);
		}
		return realTestCase;
	}
}
