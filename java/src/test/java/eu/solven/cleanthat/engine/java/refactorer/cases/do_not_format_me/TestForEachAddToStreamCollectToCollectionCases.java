package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachAddToStreamCollectToCollection;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestForEachAddToStreamCollectToCollectionCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new ForEachAddToStreamCollectToCollection();
	}

	@CompareMethods
	public static class forEachIfAddAdd {
		public List<String> pre(List<String> strings) {
			List<String> r = new ArrayList<>();
			for (String s : strings)
				if (s.length() >= 5)
					r.add(s);
			return r;
		}

		public List<String> post(List<String> strings) {
			List<String> r = strings.stream()
					.filter(s -> s.length() >= 5)
					.map(s -> s)
					.collect(Collectors.toCollection(() -> new ArrayList<>()));
			return r;
		}
	}

	@CompareMethods
	public static class forEachAddAll_collection {
		public List<String> pre(Collection<String> strings) {
			List<String> r = new ArrayList<>();
			for (String i : strings)
				r.addAll(Arrays.asList(i.toLowerCase(), i.toUpperCase()));
			return r;
		}

		public List<String> post(Collection<String> strings) {
			List<String> r = strings.stream()
					.flatMap(i -> Arrays.asList(i.toLowerCase(), i.toUpperCase()).stream())
					.collect(Collectors.toCollection(() -> new ArrayList<>()));
			return r;
		}
	}

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class forEachAddAll_iterable {
		public List<String> pre(Iterable<String> strings) {
			List<String> r = new ArrayList<>();
			for (String i : strings)
				r.addAll(Arrays.asList(i.toLowerCase(), i.toUpperCase()));
			return r;
		}
	}

	@CompareMethods
	public static class forEachAddAll_array {
		public List<String> pre(String... strings) {
			List<String> r = new ArrayList<>();
			for (String i : strings)
				r.addAll(Arrays.asList(i.toLowerCase(), i.toUpperCase()));
			return r;
		}

		public List<String> post(String... strings) {
			List<String> r = Stream.of(strings)
					.flatMap(i -> Arrays.asList(i.toLowerCase(), i.toUpperCase()).stream())
					.collect(Collectors.toCollection(() -> new ArrayList<>()));
			return r;
		}
	}

	@Ignore("https://github.com/javaparser/javaparser/issues/3955")
	@CompareMethods
	public static class forEachAddAll_fromPrimitive {
		public List<String> pre(int[] integers) {
			List<String> r = new ArrayList<>();
			for (int i : integers)
				r.addAll(Arrays.asList(Integer.toString(i), Integer.toString(i + 1)));
			return r;
		}

		public List<String> post(int[] integers) {
			List<String> r = IntStream.of(integers)
					.mapToObj(i -> i)
					.flatMap(i -> Arrays.asList(Integer.toString(i), Integer.toString(i + 1)).stream())
					.collect(Collectors.toCollection(() -> new ArrayList<>()));
			return r;
		}
	}
}
