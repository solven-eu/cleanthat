package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.time.chrono.ChronoPeriod;
import java.util.Arrays;
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

import eu.solven.cleanthat.engine.java.refactorer.annotations.CaseNotYetImplemented;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LambdaIsMethodReference;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// https://www.baeldung.com/java-8-lambda-expressions-tips
public class TestLambdaIsMethodReferenceCases extends AJavaparserRefactorerCases {

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

	// TODO
	@UnmodifiedMethod
	public static class CaseConstructor {

		Set<?> pre(Stream<?> s) {
			return s.collect(Collectors.toCollection(() -> new HashSet<>()));
		}

		Set<?> post(Stream<?> s) {
			return s.collect(Collectors.toCollection(HashSet::new));
		}
	}

	// TODO
	@UnmodifiedMethod
	public static class CaseToArray {

		Object[] pre(Stream<?> s) {
			return s.toArray(i -> new Object[i]);
		}

		Object[] post(Stream<?> s) {
			return s.toArray(Object[]::new);
		}
	}

	@UnmodifiedMethod
	public static class CaseCast_Generic {

		<T> Set<T> pre(Stream<?> s) {
			return s.map(o -> (T) o).collect(Collectors.toSet());
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
	public static class CaseBiFunction_onInstance {
		public void pre(Map<String, String> map) {
			Map<String, String> linkedHashMap = new LinkedHashMap<>();
			map.forEach((a, b) -> linkedHashMap.put(a, b));
		}

		public void post(Map<String, String> map) {
			Map<String, String> linkedHashMap = new LinkedHashMap<>();
			map.forEach(linkedHashMap::put);
		}
	}

	// TODO
	@UnmodifiedMethod
	public static class CaseBiFunction_onClass {
		final List<Integer> numbers = Arrays.asList(5, 3, 50, 24, 40, 2, 9, 18);

		public void pre() {
			numbers.stream().sorted((a, b) -> a.compareTo(b));
		}

		public void post() {
			numbers.stream().sorted(Integer::compareTo);
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

	@UnmodifiedMethod
	public static class CaseCastToGeneric {
		public List<?> pre(Stream<? extends Class<?>> s) {
			return s.map(m -> (Class<? extends CharSequence>) m).collect(Collectors.toList());
		}
	}

	// https://www.baeldung.com/java-method-references#examples-limitations
	@UnmodifiedMethod
	public static class refToExternalVariables {

		public void pre(List<String> numbers) {
			numbers.forEach(
					b -> System.out.printf("lowerCase: '%s' upperCase: '%d'%n", b.toLowerCase(), b.toUpperCase()));
		}
	}

	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import java.util.Map;\n"
					+ "\n"
					+ "public class TestEclipseStylesheetGenerator_OverBigFiles {\n"
					+ "\n"
					+ "	public String testRoaringBitmap(Map<String, ?> map) {\n"
					+ "		return map.entrySet().stream().map(e -> e.getKey()).findAny().get();\n"
					+ "	}\n"
					+ "}\n"
					+ "",
			post = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import java.util.Map;\n"
					+ "\n"
					+ "public class TestEclipseStylesheetGenerator_OverBigFiles {\n"
					+ "\n"
					+ "	public String testRoaringBitmap(Map<String, ?> map) {\n"
					+ "		return map.entrySet().stream().map(java.util.Map.Entry::getKey).findAny().get();\n"
					+ "	}\n"
					+ "}\n"
					+ "")
	public static class CaseNeedImport {
	}

	// One may prefer `Map.Entry::getKey` than `Entry::getKey`
	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import java.util.Map;\n"
					+ "import java.util.Map.Entry;\n"
					+ "\n"
					+ "public class TestEclipseStylesheetGenerator_OverBigFiles {\n"
					+ "\n"
					+ "	public String testRoaringBitmap(Entry<String, ?> e) {\n"
					+ "		return e.getKey();\n"
					+ "	}\n"
					+ "\n"
					+ "	public String testRoaringBitmap(Map<String, ?> map) {\n"
					+ "		return map.entrySet().stream().map(e -> e.getKey()).findAny().get();\n"
					+ "	}\n"
					+ "}\n"
					+ "",
			post = "package eu.solven.cleanthat.engine.java.refactorer;\n" + "\n"
					+ "import java.util.Map;\n"
					+ "import java.util.Map.Entry;\n"
					+ "\n"
					+ "public class TestEclipseStylesheetGenerator_OverBigFiles {\n"
					+ "\n"
					+ "	public String testRoaringBitmap(Entry<String, ?> e) {\n"
					+ "		return e.getKey();\n"
					+ "	}\n"
					+ "\n"
					+ "	public String testRoaringBitmap(Map<String, ?> map) {\n"
					+ "		return map.entrySet().stream().map(Entry::getKey).findAny().get();\n"
					+ "	}\n"
					+ "}\n"
					+ "")
	public static class CaseAlreadyImported {
	}

	@CompareMethods
	@CaseNotYetImplemented
	public static class Eclipse_579393 {
		public interface B<T extends A> extends A {
			T getT();
		}

		public interface A {
			default boolean exists_testOpen() {
				return true;
			}
		}

		public A pre(B<?>... sources) {
			return Stream.of(sources).map(B::getT).filter(x -> x.exists_testOpen()).findFirst().orElse(null);
		}

		public A post(B<?>... sources) {
			return Stream.of(sources).map(B::getT).filter(A::exists_testOpen).findFirst().orElse(null);
		}
	}
}
