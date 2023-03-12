package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.function.Supplier;

import com.google.common.base.Strings;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.GuavaStringsIsNullOrEmpty;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class GuavaStringsIsNullOrEmptyCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new GuavaStringsIsNullOrEmpty();
	}

	@CompareMethods
	public static class CaseNullElseIsEmpty {
		public boolean pre(String s) {
			return s == null || s.isEmpty();
		}

		public boolean post(String s) {
			return Strings.isNullOrEmpty(s);
		}
	}

	@CompareMethods
	public static class CaseIsEmptyElseNull {
		public boolean pre(String s) {
			return s.isEmpty() || s == null;
		}

		public boolean post(String s) {
			return Strings.isNullOrEmpty(s);
		}
	}

	@UnmodifiedMethod
	public static class CaseNullNotOrIsEmpty {
		public boolean pre(String s) {
			return s == null ^ s.isEmpty();
		}
	}

	@UnmodifiedMethod
	public static class CaseNullElseOtherIsEmpty {
		public boolean pre(String s, String s2) {
			return s == null || s2.isEmpty();
		}
	}

	@CompareMethods
	public static class CaseComplexExpression {
		public boolean pre(Supplier<String> s) {
			return s.get() == null || s.get().isEmpty();
		}

		public boolean post(Supplier<String> s) {
			return Strings.isNullOrEmpty(s.get());
		}
	}
}
