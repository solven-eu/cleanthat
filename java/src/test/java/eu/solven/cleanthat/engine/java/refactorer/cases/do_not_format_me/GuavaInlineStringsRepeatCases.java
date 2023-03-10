package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaInlineStringsRepeat;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class GuavaInlineStringsRepeatCases extends AJavaparserRefactorerCases {
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
			return "abc".repeat(3);
		}
	}

	@CompareMethods
	public static class CaseString {
		public Object pre(String o) {
			return o.toString();
		}

		public Object post(String o) {
			return o;
		}
	}

	@UnmodifiedMethod
	public static class CaseCharSequence {
		public Object pre(CharSequence o) {
			return o.toString();
		}

		public Object post(CharSequence o) {
			return o.toString();
		}
	}

	@CompareMethods
	public static class CaseOnLambda {
		public Object pre(List<String> input) {
			return input.stream().filter(s -> !s.toString().isEmpty()).collect(Collectors.toList());
		}

		public Object post(List<String> input) {
			return input.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
		}
	}
}
