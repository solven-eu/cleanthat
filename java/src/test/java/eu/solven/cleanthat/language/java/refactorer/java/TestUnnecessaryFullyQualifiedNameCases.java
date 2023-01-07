package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import eu.solven.cleanthat.language.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.language.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.language.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.language.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.UnnecessaryFullyQualifiedNameCases;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;
import eu.solven.cleanthat.language.java.refactorer.test.LocalClassTestHelper;

@RunWith(Parameterized.class)
public class TestUnnecessaryFullyQualifiedNameCases extends ATestCases {

	// https://github.com/junit-team/junit4/wiki/parameterized-tests
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() throws IOException {
		UnnecessaryFullyQualifiedNameCases testCases = new UnnecessaryFullyQualifiedNameCases();
		String path = LocalClassTestHelper.loadClassAsString(testCases.getClass());

		JavaParser javaParser = JavaRefactorer.makeDefaultJavaParser(testCases.getTransformer().isJreOnly());
		CompilationUnit compilationUnit = javaParser.parse(path).getResult().get();

		List<Object[]> individualCases = new ArrayList<>();

		getAllCases(compilationUnit).stream()
				.map(t -> new Object[] { javaParser, t.getName().toString(), t })
				.forEach(individualCases::add);

		return individualCases;
	}

	final JavaParser javaParser;

	final ClassOrInterfaceDeclaration testCase;

	public TestUnnecessaryFullyQualifiedNameCases(JavaParser javaParser,
			String testName,
			ClassOrInterfaceDeclaration testCase) {
		this.javaParser = javaParser;
		this.testCase = testCase;
	}

	@Test
	public void testCase() {
		Assume.assumeFalse("Ignored", testCase.getAnnotationByClass(Ignore.class).isPresent());

		UnnecessaryFullyQualifiedNameCases cases = new UnnecessaryFullyQualifiedNameCases();
		IClassTransformer transformer = cases.getTransformer();

		if (testCase.getAnnotationByClass(CompareMethods.class).isPresent()) {
			doTestMethod(transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareTypes.class).isPresent()) {
			doCompareTypes(transformer, testCase);
		}

		if (testCase.getAnnotationByClass(CompareClasses.class).isPresent()) {
			doCompareClasses(javaParser, transformer, testCase);
		}

		if (testCase.getAnnotationByClass(UnchangedMethod.class).isPresent()) {
			doCheckUnchanged(transformer, testCase);
		}
	}
}
