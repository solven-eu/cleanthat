package eu.solven.cleanthat.openrewrite.cases.do_not_format_me;

import java.util.Arrays;

import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.Environment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.CompilationUnit;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.engine.java.refactorer.AAstRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteMutator;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.cases.AParameterizesRefactorerCases;

public class TestCommonStaticAnalysisCases extends AParameterizesRefactorerCases<J.CompilationUnit, Result> {
	final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());

	@Override
	public OpenrewriteMutator getTransformer() {
		Environment environment = Environment.builder().scanRuntimeClasspath().build();
		Recipe recipe = environment.activateRecipes("org.openrewrite.java.cleanup.CommonStaticAnalysis");

		return new OpenrewriteMutator(recipe);
	}

	@Override
	protected J.CompilationUnit convertToAst(Node pre) {
		var asString = pre.toString();

		return AAstRefactorer.parse(refactorer, asString)
				.orElseThrow(() -> new IllegalArgumentException("Invalid input"));
	}

	@Override
	protected String resultToString(Result post) {
		return post.getAfter().printAll();
	}

	@Override
	protected String astToString(CompilationUnit asAst) {
		return asAst.printAll();
	}

	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.do_not_format_me;" + "public class SomeClass {"
					+ "	public SomeClass() {"
					+ "		{}"
					+ "		;;"
					+ "	}"
					+ "}",
			post = "package eu.solven.cleanthat.do_not_format_me;" + "public class SomeClass {"
					+ "	public SomeClass() {"
					+ "	}"
					+ "}")
	public static class CaseTypes {
	}
}
