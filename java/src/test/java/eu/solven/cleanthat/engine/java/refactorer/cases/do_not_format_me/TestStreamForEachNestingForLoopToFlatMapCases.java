package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamForEachNestingForLoopToFlatMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStreamForEachNestingForLoopToFlatMapCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new StreamForEachNestingForLoopToFlatMap();
	}

	// https://jsparrow.github.io/rules/flat-map-instead-of-nested-loops.html#iterating-over-nested-collections
	@CompareMethods
	public static class NestedCollections {
		public void pre(List<List<String>> values) {
			values.forEach(value -> {
				value.forEach(user -> {
					System.out.println(user);
				});
			});
		}

		public void post(List<List<String>> values) {
			values.stream().flatMap(value -> value.stream()).forEach(user -> {
				System.out.println(user);
			});
		}
	}

	@CompareMethods
	public static class NestedCollections_outerIsStream {
		public void pre(Stream<List<String>> values) {
			values.forEach(value -> {
				value.forEach(user -> {
					System.out.println(user);
				});
			});
		}

		public void post(Stream<List<String>> values) {
			values.flatMap(value -> value.stream()).forEach(user -> {
				System.out.println(user);
			});
		}
	}

	@CompareMethods
	public static class NestedCollections_innerIsStream {
		public void pre(List<Stream<String>> values) {
			values.forEach(value -> {
				value.forEach(user -> {
					System.out.println(user);
				});
			});
		}

		public void post(List<Stream<String>> values) {
			values.stream().flatMap(value -> value).forEach(user -> {
				System.out.println(user);
			});
		}
	}

	@UnmodifiedMethod
	public static class NestedCollections_outerIsIterable {
		public void pre(Iterable<List<String>> values) {
			values.forEach(value -> {
				value.forEach(user -> {
					System.out.println(user);
				});
			});
		}
	}

	// https://jsparrow.github.io/rules/flat-map-instead-of-nested-loops.html#nested-stream-foreach
	@CompareMethods
	public static class NestedCollectionForEach {
		public void pre(List<String> values) {
			values.stream().map(s -> Arrays.asList(s, s.substring(1))).forEach(items -> {
				items.forEach(item -> {
					System.out.println(item);
				});
			});
		}

		public void post(List<String> values) {
			values.stream()
					.map(s -> Arrays.asList(s, s.substring(1)))
					.flatMap(items -> items.stream())
					.forEach(item -> {
						System.out.println(item);
					});
		}
	}

	// https://jsparrow.github.io/rules/flat-map-instead-of-nested-loops.html#deep-nested-loops
	@CompareMethods
	public static class DeepNestedLoops {
		public void pre(List<List<List<List<String>>>> matrix3) {
			matrix3.stream().filter(row -> !row.isEmpty()).forEach(row -> {
				row.stream().filter(col -> !col.isEmpty()).forEach(col -> {
					col.stream().filter(cell -> !cell.isEmpty()).forEach(cell -> {
						cell.stream()
								.filter(element -> !element.isEmpty())
								.map(element -> element.substring(0, 1))
								.forEach(element -> {
									System.out.print(element);
								});
					});
				});
			});
		}

		public void post(List<List<List<List<String>>>> matrix3) {
			matrix3.stream()
					.filter(row -> !row.isEmpty())
					.flatMap(row -> row.stream().filter(col -> !col.isEmpty()))
					.flatMap(col -> col.stream().filter(cell -> !cell.isEmpty()))
					.flatMap(cell -> cell.stream()
							.filter(element -> !element.isEmpty())
							.map(element -> element.substring(0, 1)))
					.forEach(element -> {
						System.out.print(element);
					});
		}
	}
}
