package eu.solven.cleanthat.openrewrite.cases.do_not_format_me;

import java.util.Arrays;

import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.engine.java.refactorer.AAstRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteMutator;
import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.cases.AParameterizesRefactorerCases;

public class TestCommonStaticAnalysisCases extends AParameterizesRefactorerCases<SourceFile, Result> {
	final OpenrewriteRefactorer refactorer = new OpenrewriteRefactorer(Arrays.asList());

	@Override
	public OpenrewriteMutator getTransformer() {
		Environment environment = Environment.builder().scanRuntimeClasspath("org.openrewrite.staticanalysis").build();
		Recipe recipe = environment.activateRecipes("org.openrewrite.staticanalysis.CommonStaticAnalysis");

		return new OpenrewriteMutator(recipe);
	}

	@Override
	public SourceFile convertToAst(Node pre) {
		var asString = pre.toString();

		return AAstRefactorer.parse(refactorer, asString)
				.orElseThrow(() -> new IllegalArgumentException("Invalid input"));
	}

	@Override
	public String resultToString(Result post) {
		return post.getAfter().printAll();
	}

	@Override
	public String astToString(SourceFile asAst) {
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
