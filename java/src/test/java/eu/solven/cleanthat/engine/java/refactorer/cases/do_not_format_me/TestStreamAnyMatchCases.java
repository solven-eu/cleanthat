package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamAnyMatch;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStreamAnyMatchCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new StreamAnyMatch();
	}

	@CompareMethods
	public static class CaseFindAnyIsPresent {
		public Object pre(List<?> input) {
			return input.stream().filter(o -> null != o).findAny().isPresent();
		}

		public Object post(List<?> input) {
			return input.stream().anyMatch(o -> null != o);
		}
	}

	@CompareMethods
	public static class CaseFindAnyIsEmpty {
		public Object pre(List<?> input) {
			return input.stream().filter(o -> null != o).findAny().isEmpty();
		}

		public Object post(List<?> input) {
			return !input.stream().anyMatch(o -> null != o);
		}
	}

	@CompareMethods
	public static class CaseFindAnyIsPresent_usedRightAway {
		public Object pre(List<?> input) {
			return input.stream().filter(o -> null != o).findAny().isPresent() == true;
		}

		public Object post(List<?> input) {
			return input.stream().anyMatch(o -> null != o) == true;
		}
	}

	@CompareMethods
	public static class CaseFindAnyIsEmpty_usedRightAway {
		public Object pre(List<?> input) {
			return input.stream().filter(o -> null != o).findAny().isEmpty() == true;
		}

		public Object post(List<?> input) {
			return !input.stream().anyMatch(o -> null != o) == true;
		}
	}

}
