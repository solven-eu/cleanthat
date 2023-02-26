package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseTextBlocks;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class UseTextBlocksCases extends AJavaparserRefactorerCases {
	static final String PREFIX = "source/do_not_format_me/UseTextBlocks/";

	@Override
	public IJavaparserMutator getTransformer() {
		return new UseTextBlocks();
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "html_Pre.java", post = PREFIX + "html_Post.java")
	public static class caseHtml {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "MissingTrailingEol_Dirty.java",
			post = PREFIX + "MissingTrailingEol_Clean.java")
	public static class caseMissingTrailingEol {
	}

	@CompareCompilationUnitsAsResources(pre = PREFIX + "withIntegers_Dirty.java",
			post = PREFIX + "withIntegers_Clean.java")
	public static class caseWithIntegersEol {
	}

	@UnmodifiedMethod
	public static class caseUnmodified {
		public String pre() {
			return "a" + "b";
		}
	}

}
