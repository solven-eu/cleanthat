package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.ArraysDotStream;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class TestArraysDotStreamCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserMutator getTransformer() {
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

	@CompareMethods
	// @UnmodifiedMethod
	public static class EmptyArray {
		public Object pre() {
			return Arrays.asList().stream();
		}

		public Object post() {
			return Stream.of();
		}
	}

	@CompareMethods
	// @UnmodifiedMethod
	public static class ConstantArray {
		public Object pre() {
			return Arrays.asList("a", 1).stream();
		}

		public Object post() {
			return Stream.of("a", 1);
		}
	}

	@CompareMethods
	// @UnmodifiedMethod
	public static class IndividualArguments {
		public Object pre(String a, Number b) {
			return Arrays.asList(a, b).stream();
		}

		public Object post(String a, Number b) {
			return Stream.of(a, b);
		}
	}

	@CompareMethods
	// @UnmodifiedMethod
	public static class ArraysAsListString {
		public Object pre() {
			return Arrays.asList(" total 65512K").stream().collect(Collectors.joining("\n"));
		}

		public Object post() {
			return Stream.of(" total 65512K").collect(Collectors.joining("\n"));
		}
	}

	@UnmodifiedMethod
	public static class GuavaListsAsList {
		public Object pre(Runnable first, Runnable... more) {
			return Lists.asList(first, more).stream();
		}
	}

	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream() {\n"
					+ "		return Arrays.asList(\"a\", \"b\").stream().collect(Collectors.toList());\n"
					+ "	}\n"
					+ "}\n"
					+ "",
			post = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "import java.util.stream.Stream;\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream() {\n"
					+ "		return Stream.of(\"a\", \"b\").collect(Collectors.toList());\n"
					+ "	}\n"
					+ "}\n"
					+ "")
	public static class AddImports_noStreamYet {
	}

	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "import java.util.stream.Stream;\n"
					+ "\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream() {\n"
					+ "		return Arrays.asList(\"a\", \"b\").stream().collect(Collectors.toList());\n"
					+ "	}\n"
					+ "	\n"
					+ "	public static long mongoSteam(Stream<?> javaUtilStream) {\n"
					+ "		return javaUtilStream.count();\n"
					+ "	}\n"
					+ "}\n"
					+ "",
			post = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "import java.util.stream.Stream;\n"
					+ "\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream() {\n"
					+ "		return Stream.of(\"a\", \"b\").collect(Collectors.toList());\n"
					+ "	}\n"
					+ "	\n"
					+ "	public static long mongoSteam(Stream<?> javaUtilStream) {\n"
					+ "		return javaUtilStream.count();\n"
					+ "	}\n"
					+ "}\n"
					+ "")
	public static class AddImports_alreadyStream {
	}

	@CompareCompilationUnitsAsStrings(
			pre = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "\n"
					+ "import com.mongodb.connection.Stream;\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream() {\n"
					+ "		return Arrays.asList(\"a\", \"b\").stream().collect(Collectors.toList());\n"
					+ "	}\n"
					+ "	\n"
					+ "	public static String mongoSteam(Stream mongoStream) {\n"
					+ "		return mongoStream.toString();\n"
					+ "	}\n"
					+ "}\n"
					+ "",
			post = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "\n"
					+ "import com.mongodb.connection.Stream;\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream() {\n"
					+ "		return java.util.stream.Stream.of(\"a\", \"b\").collect(Collectors.toList());\n"
					+ "	}\n"
					+ "	\n"
					+ "	public static String mongoSteam(Stream mongoStream) {\n"
					+ "		return mongoStream.toString();\n"
					+ "	}\n"
					+ "}\n"
					+ "")
	public static class AddImports_hasOtherStreamAlready {
	}

	// BEWARE One may argue the type is partially available through imports
	// @CompareCompilationUnitsAsStrings
	@UnmodifiedCompilationUnitAsString(
			pre = "package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;\n" + "\n"
					+ "import java.util.Arrays;\n"
					+ "import java.util.List;\n"
					+ "import java.util.stream.Collectors;\n"
					+ "import com.fancy_editor.ILocation;\n"
					+ "\n"
					+ "public class SimpleArraysAsList {\n"
					+ "	public static List<String> toStream(ILocation location) {\n"
					+ "		return Arrays.asList(location).stream();\n"
					+ "	}\n"
					+ "}\n"
					+ "")
	public static class OverUnknownType {
	}

}
