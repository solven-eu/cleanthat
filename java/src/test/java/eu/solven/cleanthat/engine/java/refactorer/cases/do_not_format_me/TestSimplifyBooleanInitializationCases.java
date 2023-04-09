package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanInitialization;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestSimplifyBooleanInitializationCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new SimplifyBooleanInitialization();
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-anymatch
	@CompareMethods
	public static class breakToStreamAnyMatch {
		public boolean pre(List<String> strings) {
			boolean containsEmpty = false;
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
				containsEmpty = true;
			}
			return containsEmpty;
		}

		public boolean post(List<String> strings) {
			boolean containsEmpty = strings.stream().anyMatch(value -> value.isEmpty());
			return containsEmpty;
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-nonematch
	@CompareMethods
	public static class breakToStreamNoMatch {
		public boolean pre(List<String> strings) {
			boolean noneEmpty = true;
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
				noneEmpty = false;
			}
			return noneEmpty;
		}

		public boolean post(List<String> strings) {
			boolean noneEmpty = !strings.stream().anyMatch(value -> value.isEmpty());
			return noneEmpty;
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-allmatch
	@CompareMethods
	public static class breakToAllMatch {
		public boolean pre(List<String> strings) {
			boolean allEmpty = true;
			if (strings.stream().anyMatch(value -> !value.isEmpty())) {
				allEmpty = false;
			}
			return allEmpty;
		}

		public boolean post(List<String> strings) {
			boolean allEmpty = !strings.stream().anyMatch(value -> !value.isEmpty());
			return allEmpty;
		}
	}

	@CompareMethods
	// @CaseNotYetImplemented
	public static class multipleOr {
		public boolean pre(int i) {
			boolean good = false;
			if (i >= 10) {
				good = true;
			}
			if (i < 0) {
				good = true;
			}
			return good;
		}

		public boolean post(int i) {
			boolean good = i >= 10;
			if (i < 0) {
				good = true;
			}
			return good;
		}

		// TODO This could be an improvement
		public boolean post_withOr(int i) {
			boolean good = i >= 10 || i <= 0;
			return good;
		}
	}

	@CompareMethods
	public static class NeedBlock_equals {
		public boolean pre(int i) {
			boolean moveForward = true;
			if (i > 0) {
				moveForward = false;
			}
			return moveForward;
		}

		public boolean post(int i) {
			boolean moveForward = !(i > 0);
			return moveForward;
		}
	}

	@CompareMethods
	public static class NeedBlock_or {
		public boolean pre(int i) {
			boolean moveForward = true;
			if (i > 10 || i < 0) {
				moveForward = false;
			}
			return moveForward;
		}

		public boolean post(int i) {
			boolean moveForward = !(i > 10 || i < 0);
			return moveForward;
		}
	}

	@CompareMethods
	public static class NeedBlock_Ternary {
		public boolean pre(int i) {
			boolean moveForward = true;
			if (i > 10 ? true : false) {
				moveForward = false;
			}
			return moveForward;
		}

		public boolean post(int i) {
			boolean moveForward = !(i > 10 ? true : false);
			return moveForward;
		}
	}

}
