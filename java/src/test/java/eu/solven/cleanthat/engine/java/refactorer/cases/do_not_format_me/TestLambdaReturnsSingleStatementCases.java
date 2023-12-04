package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.mockito.Mockito;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaReturnsSingleStatement;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestLambdaReturnsSingleStatementCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserAstMutator getTransformer() {
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
	@UnmodifiedMethod
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

	@CompareMethods
	public static class InLambda {
		public void pre(ICodeProvider cp) throws IOException {
			cp.listFilesForContent(file -> {
				Assertions.fail("The FS is empty");
			});
		}

		public void post(ICodeProvider cp) throws IOException {
			cp.listFilesForContent(file -> Assertions.fail("The FS is empty"));
		}
	}

	@CompareMethods
	public static class InLambda_multipleArgs {
		public void pre(Map<?, ?> map) throws IOException {
			map.forEach((k, v) -> {
				System.out.println(k + ": " + v);
			});
		}

		public void post(Map<?, ?> map) throws IOException {
			map.forEach((k, v) -> System.out.println(k + ": " + v));
		}
	}

	// Comments are difficult to manage: we do not transform
	@UnmodifiedMethod
	public static class CaseWithComment {
		public Consumer<Integer> pre() {
			return x -> {
				// inner comment
				System.out.println(x + 1);
			};
		}

		public Consumer<Integer> post() {
			// inner comment
			return x -> System.out.println(x + 1);
		}
	}

	@UnmodifiedMethod
	public static class CaseWithComment_2 {
		public void pre(Stream<String> stream) {
			stream.forEach(accountId -> {
				// inner comment
				System.out.println(accountId);
			});
		}
	}
}
