package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamMapIdentity;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStreamMapIdentityCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new StreamMapIdentity();
	}

	@CompareMethods
	public static class Nominal {
		public Set<String> pre(List<String> values) {
			return values.stream().map(s -> s).collect(Collectors.toSet());
		}

		public Set<String> post(List<String> values) {
			return values.stream().collect(Collectors.toSet());
		}
	}

	@CompareMethods
	public static class Nominal_enclosed {
		public Set<String> pre(List<String> values) {
			return values.stream().map((s) -> s).collect(Collectors.toSet());
		}

		public Set<String> post(List<String> values) {
			return values.stream().collect(Collectors.toSet());
		}
	}

	@UnmodifiedMethod
	public static class MapToOtherVariable {
		public Set<String> pre(List<String> values, String k) {
			return values.stream().map(s -> k).collect(Collectors.toSet());
		}
	}

	@CompareMethods
	// https://github.com/javaparser/javaparser/issues/3995
	public static class CaseIntStream {
		public OptionalDouble pre(int[] values) {
			return IntStream.of(values).map(s -> s).average();
		}

		public OptionalDouble post(int[] values) {
			return IntStream.of(values).average();
		}
	}
}
