package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EnhancedForLoopToStreamAnyMatch;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class EnhancedForLoopToStreamAnyMatchCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new EnhancedForLoopToStreamAnyMatch();
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-anymatch
	@CompareMethods
	public static class breakToStreamAnyMatch {
		public boolean pre(List<String> strings) {
			boolean containsEmpty = false;
			for (String value : strings) {
				if (value.isEmpty()) {
					containsEmpty = true;
					break;
				}
			}
			return containsEmpty;
		}

		public boolean post(List<String> strings) {
			boolean containsEmpty = false;
			if (strings.stream().anyMatch( value -> value.isEmpty())) {
				containsEmpty = true;
			}
			return containsEmpty;
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-nonematch
	@CompareMethods
	public static class breakToStreamNoMatch {
		public boolean pre(List<String> strings) {
			boolean noneEmpty = true;
			for (String value : strings) {
				if (value.isEmpty()) {
					noneEmpty = false;
					break;
				}
			}
			return noneEmpty;
		}

		public boolean post(List<String> strings) {
			boolean noneEmpty = true;
			if (strings.stream().anyMatch( value -> value.isEmpty())) {
				noneEmpty = false;
			}
			return noneEmpty;
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-break-statement-to-stream-allmatch
	@CompareMethods
	public static class breakToAllMatch {
		public boolean pre(List<String> strings) {
			boolean allEmpty = true;
			for (String value : strings) {
				if (!value.isEmpty()) {
					allEmpty = false;
					break;
				}
			}
			return allEmpty;
		}

		public boolean post(List<String> strings) {
			boolean allEmpty = true;
			if (strings.stream().anyMatch( value -> !value.isEmpty())) {
				allEmpty = false;
			}
			return allEmpty;
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html#loop-with-return-statement
	@CompareMethods
	public static class CaseNotString {
		String someString;

		public boolean pre(List<String> strings) {
			for (String value : strings) {
				if (someString.equals(value)) {
					return true;
				}
			}
			return false;
		}

		public boolean post(List<String> strings) {
			if (strings.stream().anyMatch( value -> someString.equals(value))) {
					return true;
				}
			return false;
		}
	}
}
