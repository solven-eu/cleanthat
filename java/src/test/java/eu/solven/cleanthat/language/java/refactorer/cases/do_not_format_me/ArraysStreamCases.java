package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Ignore;

import eu.solven.cleanthat.language.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.ArraysDotStream;
import eu.solven.cleanthat.language.java.refactorer.test.ARefactorerCases;

public class ArraysStreamCases extends ARefactorerCases {
	@Override
	public IClassTransformer getTransformer() {
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

	@Ignore("TODO")
	@CompareMethods
	public static class EmptyArray {
		public Object pre() {
			return Arrays.asList().stream();
		}

		public Object post(String[] objects) {
			return Stream.of();
		}
	}

	@Ignore("TODO")
	@CompareMethods
	public static class ConstantArray {
		public Object pre() {
			return Arrays.asList("a", 1).stream();
		}

		public Object post() {
			return Stream.of("a", 1);
		}
	}

	@Ignore("TODO")
	@CompareMethods
	public static class IndividualArguments {
		public Object pre(String a, Number b) {
			return Arrays.asList(a, b).stream();
		}

		public Object post(String a, Number b) {
			return Stream.of(a, b);
		}
	}
}
