package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseStringIsEmpty;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class UseStringIsEmptyCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new UseStringIsEmpty();
	}

	@UnmodifiedMethod
	public static class CaseCollection {
		public Object pre(Collection<?> input) {
			return input.size() == 0;
		}
	}

	@UnmodifiedMethod
	public static class CaseList {
		public Object pre(List<?> input) {
			return input.size() == 0;
		}
	}

	@UnmodifiedMethod
	public static class CaseMap {
		public Object pre(Map<?, ?> input) {
			return input.size() == 0;
		}
	}

	@CompareMethods
	public static class CaseString {
		public Object pre(String input) {
			return input.length() == 0;
		}

		public Object post(String input) {
			return input.isEmpty();
		}
	}

	@CompareMethods
	public static class CaseStringEqualsEmpty {
		public Object pre(String input) {
			return input.equals("");
		}

		public Object post(String input) {
			return input.isEmpty();
		}
	}

	@UnmodifiedMethod
	public static class CaseObjectEqualsEmpty {
		public Object pre(Object input) {
			return input.equals("");
		}
	}

	@UnmodifiedMethod
	public static class CaseEmptyEqualsString {
		public Object pre(String input) {
			return "".equals(input);
		}

		public Object post(String input) {
			return input.isEmpty();
		}
	}

	@CompareMethods
	public static class CaseStringEqualsIgnoreCaseEmpty {
		public Object pre(String input) {
			return input.equalsIgnoreCase("");
		}

		public Object post(String input) {
			return input.isEmpty();
		}
	}

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class CaseString_NotEmpty {
		public Object pre(String input) {
			return input.length() != 0;
		}

		public Object post(String input) {
			return !input.isEmpty();
		}
	}
}
