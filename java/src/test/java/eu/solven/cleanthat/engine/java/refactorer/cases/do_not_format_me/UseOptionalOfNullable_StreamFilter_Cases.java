package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Arrays;
import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.CompositeJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedVariableToMap;
import eu.solven.cleanthat.engine.java.refactorer.mutators.NullCheckToOptionalOfNullable;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// https://dzone.com/articles/optional-in-java-a-swiss-army-knife-for-handling-n
public class UseOptionalOfNullable_StreamFilter_Cases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new CompositeJavaparserMutator(Arrays.asList(new NullCheckToOptionalOfNullable(),
				new OptionalWrappedVariableToMap(),
				new LambdaIsMethodReference()));
	}

	@CompareMethods
	public static class ChainedConditions {
		public void pre(String s) {
			if (s != null) {
				String v = s.toUpperCase();
				if (v.startsWith("A")) {
					System.out.println(v);
				}
			}
		}

		public void post(String s) {
			Optional.ofNullable(s).ifPresent(s_ -> {
				String v = s_.toUpperCase();
				if (v.startsWith("A")) {
					System.out.println(v);
				}
			});
		}

		public void postWithXXX(String s) {
			Optional.ofNullable(s)
					.map(v -> v.toUpperCase())
					.filter(v -> v.startsWith("A"))
					.ifPresent(v -> System.out.println(v));
		}
	}

	@CompareMethods
	public static class NewVariableNameConflict {
		public void pre(String s) {
			String s_ = "AlreadyUsed";

			if (s != null) {
				String v = s.toUpperCase();
				if (v.startsWith("A")) {
					System.out.println(v);
				}
			}

			System.out.println(s_);
		}

		public void post(String s) {
			String s_ = "AlreadyUsed";

			Optional.ofNullable(s).ifPresent(s__ -> {
				String v = s__.toUpperCase();
				if (v.startsWith("A")) {
					System.out.println(v);
				}
			});

			System.out.println(s_);
		}
	}
}
