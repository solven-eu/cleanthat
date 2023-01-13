package eu.solven.cleanthat.language.java.refactorer.cases;

import java.io.IOException;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.UnnecessaryFullyQualifiedNameCases;
import eu.solven.cleanthat.language.java.refactorer.test.ARefactorerCases;

@RunWith(Parameterized.class)
public class TestUnnecessaryFullyQualifiedNameCases extends AParameterizesRefactorerCases {

	private static ARefactorerCases getStaticRefactorerCases() {
		return new UnnecessaryFullyQualifiedNameCases();
	}

	public TestUnnecessaryFullyQualifiedNameCases(JavaParser javaParser,
			String testName,
			ClassOrInterfaceDeclaration testCase) {
		super(javaParser, testName, testCase);
	}

	// https://github.com/junit-team/junit4/wiki/parameterized-tests
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() throws IOException {
		ARefactorerCases testCases = getStaticRefactorerCases();
		return listCases(testCases);
	}

	@Override
	protected ARefactorerCases getCases() {
		return getStaticRefactorerCases();
	}
}
