package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.multiple;

import java.util.Arrays;
import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.CompositeJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.NullCheckToOptionalOfNullable;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedVariableToMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// https://dzone.com/articles/optional-in-java-a-swiss-army-knife-for-handling-n
public class TestUseOptionalOfNullable_StreamFilter_Cases extends AJavaparserRefactorerCases {
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
			Optional.ofNullable(s).map(String::toUpperCase).ifPresent(v -> {
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
}
