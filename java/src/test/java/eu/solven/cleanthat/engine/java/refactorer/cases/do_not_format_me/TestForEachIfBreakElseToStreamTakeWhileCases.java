package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachIfBreakElseToStreamTakeWhile;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestForEachIfBreakElseToStreamTakeWhileCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ForEachIfBreakElseToStreamTakeWhile();
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-take-while.html#iterating-over-a-list
	@CompareMethods
	public static class OverList {
		public void pre(List<String> values) {
			for (String user : values) {
				if (!user.isEmpty()) {
					break;
				}
				System.out.println(user);
			}
		}

		public void post(List<String> values) {
			values.stream().takeWhile(user -> !user.isEmpty()).forEach(user -> {
				System.out.println(user);
			});
		}
	}

	// https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-take-while.html#iterating-over-a-map
	@CompareMethods
	public static class OverMap {
		public void pre(Map<String, Long> values) {
			for (Entry<String, Long> entry : values.entrySet()) {
				if (!entry.getKey().isEmpty()) {
					break;
				}
				Long user = entry.getValue();
				System.out.println(user);
			}
		}

		public void post(Map<String, Long> values) {
			values.entrySet().stream().takeWhile(entry -> !entry.getKey().isEmpty()).forEach(entry -> {
				Long user = entry.getValue();
				System.out.println(user);
			});
		}
	}

	@UnmodifiedMethod
	public static class ConditionModifyExternal {
		public void pre(List<String> values) {
			int i = 0;

			for (String user : values) {
				if (!user.isEmpty() || i++ >= 2) {
					break;
				}
				System.out.println(user);
			}
		}
	}
}
