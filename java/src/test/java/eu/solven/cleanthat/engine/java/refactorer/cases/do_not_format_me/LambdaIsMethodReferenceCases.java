package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.chrono.ChronoPeriod;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	@CompareMethods
	public static class Case_methodCall {

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

	// TODO
	@UnmodifiedMethod
	// @CompareMethods
	public static class Case_methodCall_afterSimpleMap {

		class B extends CaseSonar {
			<T> T getObject() {
				return null;
			}
		}

		Stream<?> pre(List<B> list) {
			return list.stream().map(b -> b).map(b -> b.<String>getObject());
		}

		Stream<?> post(List<B> list) {
			return list.stream().map(b -> b).map(B::<String>getObject);
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
	public static class CasemethodCall_notOverLambda {
		public ObjectMapper pre(List<ObjectMapper> objectMappers) {
			return objectMappers.stream()
					.filter(om -> JsonFactory.FORMAT_NAME_JSON.equals(om.getFactory().getFormatName()))
					.findAny()
					.get();
		}
	}

	@UnmodifiedMethod
	public static class CaseInstanceOf_notOverLambda {
		public ObjectMapper pre(List<ObjectMapper> objectMappers) {
			return objectMappers.stream().filter(om -> objectMappers instanceof List).findAny().get();
		}
	}

	@UnmodifiedMethod
	public static class CaseCastOf_notOverLambda {
		public String pre(List<ChronoPeriod> chronoPeriods) {
			Object something = "";

			return chronoPeriods.stream().map(cp -> (String) something).findAny().get();
		}
	}

	// @CompareMethods
	// Stuck on https://github.com/javaparser/javaparser/issues/3929
	@UnmodifiedMethod
	public static class CaseBiFunction {
		public void pre(Map<String, String> map) {
			Map<String, String> linkedHashMap = new LinkedHashMap<>();
			map.forEach((a, b) -> linkedHashMap.put(a, b));
		}

		public void post(Map<String, String> map) {
			Map<String, String> linkedHashMap = new LinkedHashMap<>();
			map.forEach(linkedHashMap::put);
		}
	}

	// Example from https://rules.sonarsource.com/java/RSPEC-1602
	// Stuck on https://github.com/javaparser/javaparser/issues/3929
	@CompareMethods
	// @UnmodifiedMethod
	public static class CaseRunnable {
		public void pre(List<Runnable> runnables) {
			runnables.forEach(r -> r.run());
		}

		public void post(List<Runnable> runnables) {
			runnables.forEach(Runnable::run);
		}
	}
}
