package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.multiple;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.CompositeJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ForEachToIterableForEach;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaReturnsSingleStatement;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LoopIntRangeToIntStreamForEach;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalWrappedIfToFilter;
import eu.solven.cleanthat.engine.java.refactorer.mutators.StreamForEachNestingForLoopToFlatMap;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestNestedForLoopsToStreamFlatMapCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
		return new CompositeJavaparserMutator(Arrays.asList(new ForEachToIterableForEach(),
				new StreamForEachNestingForLoopToFlatMap(),
				new LoopIntRangeToIntStreamForEach(),
				new OptionalWrappedIfToFilter(),
				new LambdaIsMethodReference(),
				new LambdaReturnsSingleStatement()));
	}

	@CompareMethods
	public static class InnerIsForLoop {
		public void pre(List<String> values, Function<String, List<String>> userToOrders) {
			for (String user : values) {
				for (String order : userToOrders.apply(user)) {
					System.out.println(order);
				}
			}
		}

		public void post(List<String> values, Function<String, List<String>> userToOrders) {
			values.stream().flatMap(user -> userToOrders.apply(user).stream()).forEach(System.out::println);
		}
	}

	@CompareMethods
	public static class InnerIsForLoop_intermediateVariable {
		public void pre(List<String> values, Function<String, List<String>> userToOrders) {
			for (String user : values) {
				List<String> orders = userToOrders.apply(user);
				for (String order : orders) {
					System.out.println(order);
				}
			}
		}

		public void post(List<String> values, Function<String, List<String>> userToOrders) {
			values.forEach(user -> {
				List<String> orders = userToOrders.apply(user);
				orders.forEach(System.out::println);
			});
		}

		public void postTodo(List<String> values, Function<String, List<String>> userToOrders) {
			values.stream().flatMap(user -> userToOrders.apply(user).stream()).forEach(System.out::println);
		}
	}

	@CompareMethods
	public static class InnerIsIntLoop {
		public void pre(List<String> values) {
			for (String user : values) {
				for (int length = 0; length < user.length(); length++) {
					System.out.println(length);
				}
			}
		}

		public void post(List<String> values) {
			values.forEach(user -> IntStream.range(0, user.length()).forEach(System.out::println));
		}

		public void postTodo(List<String> values) {
			values.stream().flatMapToInt(user -> IntStream.range(0, user.length())).forEach(System.out::println);
		}
	}

	@CompareMethods
	public static class InnerIsIntStream {
		public void pre(List<String> values) {
			for (String user : values) {
				IntStream.range(0, user.length()).forEach(length -> {
					System.out.println(length);
				});
			}
		}

		public void post(List<String> values) {
			values.forEach(user -> IntStream.range(0, user.length()).forEach(System.out::println));
		}

		public void postTodo(List<String> values) {
			values.stream().flatMapToInt(user -> IntStream.range(0, user.length())).forEach(System.out::println);
		}
	}

	@CompareMethods
	public static class InnerReferToOuter {
		public void pre(List<String> values) {
			for (String user : values) {
				IntStream.range(0, user.length())
						.mapToObj(startIndex -> user.substring(startIndex))
						.forEach(subString -> {
							System.out.println(subString);
						});
			}
		}

		public void post(List<String> values) {
			values.stream()
					.flatMap(user -> IntStream.range(0, user.length())
							.mapToObj(startIndex -> user.substring(startIndex)))
					.forEach(System.out::println);
		}
	}

	@CompareMethods
	// @UnmodifiedMethod
	// TODO Should we rely on StreamSupport?
	public static class Iterables {
		public void pre(Iterable<String> values, Function<String, Iterable<String>> userToOrders) {
			for (String user : values) {
				for (String order : userToOrders.apply(user)) {
					System.out.println(order);
				}
			}
		}

		public void post(Iterable<String> values, Function<String, Iterable<String>> userToOrders) {
			values.forEach(user -> userToOrders.apply(user).forEach(System.out::println));
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
					.forEach(System.out::print);
		}

		public void postTodo(List<List<List<List<String>>>> matrix3) {
			matrix3.stream()
					.filter(row -> !row.isEmpty())
					.flatMap(row -> row.stream())
					.filter(col -> !col.isEmpty())
					.flatMap(col -> col.stream())
					.filter(cell -> !cell.isEmpty())
					.flatMap(cell -> cell.stream())
					.filter(element -> !element.isEmpty())
					.map(element -> element.substring(0, 1))
					.forEach(System.out::print);
		}
	}
}
