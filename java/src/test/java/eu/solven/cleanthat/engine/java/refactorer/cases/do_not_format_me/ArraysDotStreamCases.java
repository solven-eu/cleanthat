package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Arrays;
import java.util.stream.Stream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ArraysDotStream;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class ArraysDotStreamCases extends ARefactorerCases {
	@Override
	public IMutator getTransformer() {
		return new ArraysDotStream();
	}

	@CompareMethods
	public static class Ellipse {
		public Object pre(Object... objects) {
			return Arrays.asList(objects).stream();
		}

		public Object post(Object... objects) {
			return Arrays.stream(objects);
		}
	}

	@CompareMethods
	public static class ObjectArray {
		public Object pre(Object[] objects) {
			return Arrays.asList(objects).stream();
		}

		public Object post(Object[] objects) {
			return Arrays.stream(objects);
		}
	}

	@CompareMethods
	public static class StringArray {
		public Object pre(String[] objects) {
			return Arrays.asList(objects).stream();
		}

		public Object post(String[] objects) {
			return Arrays.stream(objects);
		}
	}

	// @CompareMethods
	@UnchangedMethod
	public static class EmptyArray {
		public Object pre() {
			return Arrays.asList().stream();
		}

		public Object post(String[] objects) {
			return Stream.of();
		}
	}

	// @CompareMethods
	@UnchangedMethod
	public static class ConstantArray {
		public Object pre() {
			return Arrays.asList("a", 1).stream();
		}

		public Object post() {
			return Stream.of("a", 1);
		}
	}

	// @CompareMethods
	@UnchangedMethod
	public static class IndividualArguments {
		public Object pre(String a, Number b) {
			return Arrays.asList(a, b).stream();
		}

		public Object post(String a, Number b) {
			return Stream.of(a, b);
		}
	}
}
