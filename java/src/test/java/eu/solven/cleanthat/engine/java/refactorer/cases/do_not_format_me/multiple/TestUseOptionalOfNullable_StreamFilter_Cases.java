package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.multiple;

import java.util.Arrays;
import java.util.Optional;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.CompositeJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaReturnsSingleStatement;
import eu.solven.cleanthat.engine.java.refactorer.mutators.NullCheckToOptionalOfNullable;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedIfToFilter;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedVariableToMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// https://dzone.com/articles/optional-in-java-a-swiss-army-knife-for-handling-n
public class TestUseOptionalOfNullable_StreamFilter_Cases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new CompositeJavaparserMutator(Arrays.asList(new NullCheckToOptionalOfNullable(),
				new OptionalWrappedVariableToMap(),
				new OptionalWrappedIfToFilter(),
				new LambdaIsMethodReference(),
				new LambdaReturnsSingleStatement()));
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
			Optional.ofNullable(s)
					.map(String::toUpperCase)
					.filter(v -> v.startsWith("A"))
					.ifPresent(System.out::println);
		}
	}
}
