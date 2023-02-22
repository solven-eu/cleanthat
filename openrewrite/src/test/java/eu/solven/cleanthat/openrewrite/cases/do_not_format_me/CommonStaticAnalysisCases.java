package eu.solven.cleanthat.openrewrite.cases.do_not_format_me;

import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.Environment;
import org.openrewrite.java.tree.J;

import eu.solven.cleanthat.engine.java.refactorer.OpenrewriteMutator;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class CommonStaticAnalysisCases extends ARefactorerCases<J.CompilationUnit, Result, OpenrewriteMutator> {
	@Override
	public OpenrewriteMutator getTransformer() {
		Environment environment = Environment.builder().scanRuntimeClasspath().build();
		Recipe recipe = environment.activateRecipes("org.openrewrite.java.cleanup.CommonStaticAnalysis");

		return new OpenrewriteMutator(recipe);
	}

	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.do_not_format_me;" + "public class CleanClass {"
					+ "	public CleanClass() {"
					+ "		{}"
					+ "		;;"
					+ "	}"
					+ "}",
			post = "package eu.solven.cleanthat.do_not_format_me;" + "public class CleanClass {"
					+ "	public CleanClass() {"
					+ "		{}"
					+ "		;;"
					+ "	}"
					+ "}")
	public static class CaseTypes {
	}
}
