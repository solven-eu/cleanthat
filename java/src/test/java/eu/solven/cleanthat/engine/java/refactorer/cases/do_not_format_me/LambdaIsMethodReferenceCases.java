package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// https://www.baeldung.com/java-8-lambda-expressions-tips
public class LambdaIsMethodReferenceCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new LambdaIsMethodReference();
	}

	@UnmodifiedMethod
	public static class helpBuilding {

		class B extends CaseSonar {
			<T> T getObject() {
				return null;
			}
		}

		void pre(List<CaseSonar> list) {
			list.stream().filter(Objects::isNull).filter(Objects::nonNull).count();

			list.stream()
					.filter(B.class::isInstance)
					.map(B.class::cast)
					.map(B::<String>getObject)
					.forEach(System.out::println);
		}
	}

	// https://sonarsource.atlassian.net/browse/RSPEC-1612

	// .map(B::<String>getObject)
	@CompareMethods
	public static class CaseSonar {

		class B extends CaseSonar {
			<T> T getObject() {
				return null;
			}
		}

		void pre(List<CaseSonar> list) {
			list.stream()
					.filter(a -> a instanceof B)
					.map(a -> (B) a)
					.map(b -> b.<String>getObject())
					.forEach(b -> System.out.println(b));
		}

		void post(List<CaseSonar> list) {
			list.stream()
					.filter(B.class::isInstance)
					.map(B.class::cast)
					.map(b -> b.<String>getObject())
					.forEach(System.out::println);
		}
	}

	// TODO
	@UnmodifiedMethod
	public static class CaseSonar_todo {

		class B extends CaseSonar {
			<T> T getObject() {
				return null;
			}
		}

		Stream<?> pre(List<B> list) {
			return list.stream().map(b -> b.<String>getObject());
		}

		Stream<?> post(List<B> list) {
			return list.stream().map(B::<String>getObject);
		}
	}

	@UnmodifiedMethod
	public static class CaseConstructor {

		Set<?> pre(Stream<?> s) {
			return s.collect(Collectors.toCollection(() -> new HashSet<>()));
		}

		Set<?> post(Stream<?> s) {
			return s.collect(Collectors.toCollection(HashSet::new));
		}
	}

	@CompareMethods
	public static class CaseNullComparison {
		public Object pre(Stream<?> s) {
			return s.filter(o -> o == null).filter(o -> o != null).filter(o -> null == null).count();
		}

		public Object post(Stream<?> s) {
			return s.filter(Objects::isNull).filter(Objects::nonNull).filter(o -> true).count();
		}
	}

	@UnmodifiedMethod
	public static class CaseEqualsComplex {
		public ObjectMapper pre(List<ObjectMapper> objectMappers) {
			return objectMappers.stream()
					.filter(om -> JsonFactory.FORMAT_NAME_JSON.equals(om.getFactory().getFormatName()))
					.findAny()
					.get();
		}
	}

	// Example from https://rules.sonarsource.com/java/RSPEC-1602
	// @CompareMethods
	// public static class CaseBiFunction {
	// public BiFunction<String, String, Integer> pre() {
	// return (a,b) ->
	// return a.substring(b.length);
	// };
	// }
	//
	// public Function<String, Integer> post() {
	// return s -> {
	// return s.length();
	// };
	// }
	// }
}
