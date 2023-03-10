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
