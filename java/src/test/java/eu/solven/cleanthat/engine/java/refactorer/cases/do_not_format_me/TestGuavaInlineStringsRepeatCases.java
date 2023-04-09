package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import com.google.common.base.Strings;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaInlineStringsRepeat;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestGuavaInlineStringsRepeatCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new GuavaInlineStringsRepeat();
	}

	@CompareMethods
	public static class CaseNotString {
		public String pre() {
			return Strings.repeat("abc", 2);
		}

		public String post() {
			return "abc".repeat(2);
		}
	}
}
