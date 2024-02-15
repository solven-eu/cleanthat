package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfBreakToStreamFindFirst;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfToIfStreamAnyMatch;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestForEachIfToIfStreamAnyMatchCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new ForEachIfToIfStreamAnyMatch();
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
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
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
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
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
			if (strings.stream().anyMatch(value -> !value.isEmpty())) {
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
			if (strings.stream().anyMatch(value -> someString.equals(value))) {
				return true;
			}
			return false;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class ReturnVariable {

		Map.Entry<String, String> pre(String filePathString, Map<String, String> idToIndex) throws IOException {
			for (Map.Entry<String, String> item : idToIndex.entrySet()) {
				if (filePathString.contains(item.getKey())) {
					return item;
				}
			}
			return null;
		}

		Map.Entry<String, String> post(String filePathString, Map<String, String> idToIndex) throws IOException {
			Optional<Map.Entry<String, String>> firstItem =
					idToIndex.entrySet().stream().filter(item -> filePathString.contains(item.getKey())).map(item -> {
						return item;
					}).findFirst();
			if (firstItem.isPresent()) {
				return firstItem.get();
			}
			return null;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class withCommentBeforeFor {
		public boolean pre(List<String> strings) {
			boolean containsEmpty = false;
			// some comment
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
			// some comment
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
				containsEmpty = true;
			}
			return containsEmpty;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class withCommentBeforeIf {
		public boolean pre(List<String> strings) {
			boolean containsEmpty = false;
			for (String value : strings) {
				// some comment
				if (value.isEmpty()) {
					containsEmpty = true;
					break;
				}
			}
			return containsEmpty;
		}

		public boolean post(List<String> strings) {
			boolean containsEmpty = false;
			// some comment
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
				containsEmpty = true;
			}
			return containsEmpty;
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class withCommentbeforeForBeforeIf {
		public boolean pre(List<String> strings) {
			boolean containsEmpty = false;
			// before for
			for (String value : strings) {
				// before if
				if (value.isEmpty()) {
					containsEmpty = true;
					break;
				}
			}
			return containsEmpty;
		}

		public boolean post(List<String> strings) {
			boolean containsEmpty = false;
			// some comment
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
				containsEmpty = true;
			}
			return containsEmpty;
		}
	}

	@CompareMethods
	public static class setVariable_ifTrue {
		public boolean pre(List<String> strings) {
			Object someObject = null;

			boolean containsEmpty = false;
			for (String value : strings) {
				if (value.isEmpty()) {
					someObject = new Object();
					containsEmpty = true;
					break;
				}
			}

			System.out.println(someObject);
			return containsEmpty;
		}

		public boolean post(List<String> strings) {
			Object someObject = null;

			boolean containsEmpty = false;
			if (strings.stream().anyMatch(value -> value.isEmpty())) {
				someObject = new Object();
				containsEmpty = true;
			}
			System.out.println(someObject);
			return containsEmpty;
		}
	}

	@UnmodifiedMethod
	public static class setVariable_duringIf {
		public boolean pre(List<String> strings) {
			Object someObject = null;

			boolean containsEmpty = false;
			for (String value : strings) {
				if (value.isEmpty() && (someObject = new Object()) != null) {
					containsEmpty = true;
					break;
				}
			}

			System.out.println(someObject);
			return containsEmpty;
		}
	}

	// This should be turned into `strings.stream().filter(value -> value.length() >= 4).findFirst().orElse("")`
	// which is managed by ForEachIfBreakToStreamFindFirst
	@UnmodifiedMethod
	public static class AssignIterated {
		public String pre(List<String> strings) {
			String key = "";
			for (String value : strings) {
				if (value.length() > 4) {
					key = value;
					break;
				}
			}

			return key;
		}
	}
}
