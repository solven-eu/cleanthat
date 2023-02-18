package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Ignore;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaReturnsSingleStatement;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class LambdaReturnsSingleStatementCases extends ARefactorerCases {

	@Override
	public IMutator getTransformer() {
		return new LambdaReturnsSingleStatement();
	}

	@CompareMethods
	public static class CaseString {
		public Object pre(List<String> l) {
			return l.stream().map(s -> {
				return s.charAt(0);
			}).findFirst().get();
		}

		public Object post(List<String> l) {
			return l.stream().map(s -> s.charAt(0)).findFirst().get();
		}
	}

	// Example from https://rules.sonarsource.com/java/RSPEC-1602
	@CompareMethods
	public static class CaseStaticCall {
		public Consumer<Integer> pre() {
			return x -> {
				System.out.println(x + 1);
			};
		}

		public Consumer<Integer> post() {
			return x -> System.out.println(x + 1);
		}
	}

	// Example from https://rules.sonarsource.com/java/RSPEC-1602
	@CompareMethods
	public static class CaseBiFunction {
		public BiFunction<Integer, Integer, Number> pre() {
			return (a, b) -> {
				return a + b;
			};
		}

		public BiFunction<Integer, Integer, Number> post() {
			return (a, b) -> a + b;
		}
	}

	// https://community.sonarsource.com/t/incorrect-inconsistent-sonar-squid-rule-regarding-lambdas-and-curly-braces-squid-s1602/17141/3
	@Ignore("TODO This may require access to the classPath")
	@UnchangedMethod
	// @CompareMethods
	public static class CaseConflictingMethods {
		public interface SomeInterface {
			<T> T asTransaction(Function<String, T> function);

			void asTransaction(Consumer<String> function);
		}

		public void pre(SomeInterface input) {
			input.asTransaction(session -> {
				session.toLowerCase();
			});

			input.asTransaction(session -> {
				return session.toLowerCase();
			});
		}

		// These transformations would not compile
		public void post(SomeInterface input) {
			// input.asTransaction(session -> session.toLowerCase());

			// input.asTransaction(session -> session.toLowerCase());
		}
	}
}
