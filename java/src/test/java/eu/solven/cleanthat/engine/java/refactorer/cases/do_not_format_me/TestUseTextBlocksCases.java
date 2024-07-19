package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsResource;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseTextBlocks;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUseTextBlocksCases extends AJavaparserRefactorerCases {
	static final String PREFIX = "source/do_not_format_me/UseTextBlocks/";

	@Override
	public IJavaparserAstMutator getTransformer() {
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

	// @CaseNotYetImplemented
	// @CompareCompilationUnitsAsResources(pre = PREFIX + "singleString_Dirty.java",
	// post = PREFIX + "singleString_Clean.java")
	@UnmodifiedCompilationUnitAsResource(pre = PREFIX + "singleString_Dirty.java")
	public static class caseSingleString {
	}

	@UnmodifiedMethod
	public static class caseUnmodified {
		public String pre() {
			return "a" + "b";
		}
	}

	@UnmodifiedMethod
	public static class caseRegexEolWindows {
		public String pre() {
			return "[\r\n]+";
		}
	}

	@UnmodifiedMethod
	public static class caseRegexEolLinux {
		public String pre() {
			return "[\n]+";
		}
	}

	@CaseNotYetImplemented
	// @UnmodifiedMethod
	public static class caseRegexEolLinuxWithConcat {
		public String pre() {
			return "[" + "\n" + "]+";
		}
	}

	@UnmodifiedMethod
	public static class caseSingleEolAtEnd {
		public String pre() {
			return "Youpi\n";
		}
	}

}
