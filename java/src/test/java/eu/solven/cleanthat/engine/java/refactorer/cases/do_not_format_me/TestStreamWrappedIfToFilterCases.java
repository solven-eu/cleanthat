package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamWrappedIfToFilter;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStreamWrappedIfToFilterCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new StreamWrappedIfToFilter();
	}

	@CompareMethods
	public static class IntStream_forEach_intermediateInt {
		public void pre(IntStream is) {
			is.forEach(i -> {
				if (i == '\'')
					System.out.println(i);
			});
		}

		public void post(IntStream is) {
			is.filter(i -> i == '\'').forEach(i -> System.out.println(i));
		}
	}

	@CompareMethods
	public static class Stream_forEach_intermediateInt {
		public void pre(Stream<String> strings) {
			strings.forEach(s -> {
				if (s.length() >= 1) {
					System.out.println(s);
				}
			});
		}

		public void post(Stream<String> strings) {
			strings.filter(s -> s.length() >= 1).forEach(s -> {
				System.out.println(s);
			});
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class ConditionNotOnVariable {
		public void pre(Stream<String> strings, boolean b) {
			strings.forEach(s -> {
				if (b) {
					System.out.println(s);
				}
			});
		}

		public void post(Stream<String> strings, boolean b) {
			if (b) {
				strings.forEach(s -> {
					System.out.println(s);
				});
			}
		}
	}

	@UnmodifiedMethod
	public static class WithElse {
		public void pre(String s) {
			IntStream.range(0, s.length()).forEach(c -> {
				if (c == '\'')
					System.out.println(s);
				else
					System.out.println("Arg");
			});
		}
	}
}
