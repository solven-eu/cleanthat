package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.UseCollectionIsEmpty;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestUseCollectionIsEmptyCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new UseCollectionIsEmpty();
	}

	@CompareMethods
	public static class CaseCollection {
		public Object pre(Collection<?> input) {
			return input.size() == 0;
		}

		public Object post(Collection<?> input) {
			return input.isEmpty();
		}
	}

	// Check we handle most Collection sub-types
	@CompareMethods
	public static class CaseList {
		public Object pre(List<?> input) {
			return input.size() == 0;
		}

		public Object post(List<?> input) {
			return input.isEmpty();
		}
	}

	@CompareMethods
	public static class CaseMap {
		public Object pre(Map<?, ?> input) {
			return input.size() == 0;
		}

		public Object post(Map<?, ?> input) {
			return input.isEmpty();
		}
	}

	@UnmodifiedMethod
	public static class CaseString {
		public Object pre(String input) {
			return input.length() == 0;
		}
	}

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class CaseCollection_NotEmpty {
		public Object pre(Collection<?> input) {
			return input.size() != 0;
		}

		public Object post(Collection<?> input) {
			return !input.isEmpty();
		}
	}
}
