package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.SimplifyBooleanInitialization;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class SimplifyBooleanInitializationCases extends AJavaparserRefactorerCases {
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
}
