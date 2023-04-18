package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.stream.Stream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamFlatMapStreamToFlatMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestStreamFlatMapStreamToFlatMapCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new StreamFlatMapStreamToFlatMap();
	}

	@CompareMethods
	public static class NestedFilter {
		public void pre(List<List<String>> matrix3) {
			matrix3.stream().flatMap(row -> row.stream().filter(col -> !col.isEmpty())).forEach(element -> {
				System.out.print(element);
			});
		}

		public void post(List<List<String>> matrix3) {
			matrix3.stream().flatMap(row -> row.stream()).filter(col -> !col.isEmpty()).forEach(element -> {
				System.out.print(element);
			});
		}
	}

	@CompareMethods
	public static class NestedFilterMap {
		public void pre(List<List<String>> matrix3) {
			matrix3.stream()
					.flatMap(row -> row.stream().filter(col -> !col.isEmpty()).map(element -> element.substring(0, 1)))
					.forEach(element -> {
						System.out.print(element);
					});
		}

		public void post(List<List<String>> matrix3) {
			matrix3.stream()
					.flatMap(row -> row.stream())
					.filter(col -> !col.isEmpty())
					.map(element -> element.substring(0, 1))
					.forEach(element -> {
						System.out.print(element);
					});
		}
	}

	@CompareMethods
	public static class OuterMap {
		public void pre(List<List<String>> matrix3) {
			matrix3.stream()
					.flatMap(row -> row.stream().filter(col -> !col.isEmpty()).map(element -> element.substring(0, 3)))
					.map(element -> element.substring(1, 2))
					.forEach(element -> {
						System.out.print(element);
					});
		}

		public void post(List<List<String>> matrix3) {
			matrix3.stream()
					.flatMap(row -> row.stream())
					.filter(col -> !col.isEmpty())
					.map(element -> element.substring(0, 3))
					.map(element -> element.substring(1, 2))
					.forEach(element -> {
						System.out.print(element);
					});
		}
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class ReturnFlatMap {
		public Stream<String> pre(List<List<String>> matrix3) {
			return matrix3.stream().flatMap(row -> row.stream().filter(col -> !col.isEmpty()));
		}

		public Stream<String> post(List<List<String>> matrix3) {
			return matrix3.stream().flatMap(row -> row.stream()).filter(col -> !col.isEmpty());
		}
	}

	// https://jsparrow.github.io/rules/flat-map-instead-of-nested-loops.html#deep-nested-loops
	@CompareMethods
	public static class DeepNestedLoops {
		public void pre(List<List<List<List<String>>>> matrix3) {
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

		public void post(List<List<List<List<String>>>> matrix3) {
			matrix3.stream()
					.filter(row -> !row.isEmpty())
					.flatMap(row -> row.stream())
					.filter(col -> !col.isEmpty())
					.flatMap(col -> col.stream())
					.filter(cell -> !cell.isEmpty())
					.flatMap(cell -> cell.stream())
					.filter(element -> !element.isEmpty())
					.map(element -> element.substring(0, 1))
					.forEach(element -> {
						System.out.print(element);
					});
		}
	}
}
