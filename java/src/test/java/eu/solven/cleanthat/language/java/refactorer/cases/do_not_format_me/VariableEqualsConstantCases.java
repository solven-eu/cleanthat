package eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me;

import java.util.List;

import org.junit.Ignore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.language.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.VariableEqualsConstant;
import eu.solven.cleanthat.language.java.refactorer.test.ACases;

public class VariableEqualsConstantCases extends ACases {

	@Override
	public IClassTransformer getTransformer() {
		return new VariableEqualsConstant();
	}

	@CompareMethods
	public static class CaseConstantString {
		public Object pre(String input) {
			return input.equals("hardcoded");
		}

		public Object post(String input) {
			return "hardcoded".equals(input);
		}
	}

	@UnchangedMethod
	public static class CaseConstantStringWithAnotherConstant {
		public Object post() {
			return "hardcoded".equals("input");
		}
	}

	@CompareMethods
	public static class CaseConstantStringAgainstObject {
		public Object pre(Object input) {
			return input.equals("hardcoded");
		}

		public Object post(Object input) {
			return "hardcoded".equals(input);
		}
	}

	// This case relies on the fact Object.equals should be reflexive.
	@CompareMethods
	public static class CaseConstantCustom_constructor {
		private static final class SomeClass {

		}

		public Object pre(Object input) {
			return input.equals(new SomeClass());
		}

		public Object post(Object input) {
			return new SomeClass().equals(input);
		}
	}

	// TODO To make this work, we would need the ability to know the argument is non-null. While it looks trivial for a
	// human, it needs additional work through JavaParser
	@UnchangedMethod
	public static class CaseConstantCustom_ref {
		private static final SomeClass constantSomeClass = new SomeClass();

		private static final class SomeClass {

		}

		public Object post(Object input) {
			return input.equals(constantSomeClass);
		}
	}

	// https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#literalsfirstincomparisons
	@CompareMethods
	public static class CasePMD_stringEquals {
		boolean pre(String x) {
			return x.equals("2");
		}

		boolean post(String x) {
			return "2".equals(x);
		}
	}

	@CompareMethods
	public static class CasePMD_stringEqualsIgnoreCase {
		boolean pre(String x) {
			return x.equalsIgnoreCase("2");
		}

		boolean post(String x) {
			return "2".equalsIgnoreCase(x);
		}
	}

	@Ignore
	@CompareMethods
	public static class CasePMD_stringCompareTo {
		boolean pre(String x) {
			return (x.compareTo("bar") > 0);
		}

		boolean post(String x) {
			return ("bar".compareTo(x) < 0);
		}

	}

	@Ignore
	@CompareMethods
	public static class CasePMD_stringCompareToIgnoreCase {
		boolean pre(String x) {
			return (x.compareToIgnoreCase("bar") > 0);
		}

		boolean post(String x) {
			return ("bar".compareToIgnoreCase(x) < 0);
		}
	}

	@CompareMethods
	public static class CasePMD_stringContentEquals {
		boolean pre(String x) {
			return x.contentEquals("bar");
		}

		boolean post(String x) {
			return "bar".contentEquals(x);
		}
	}

	@UnchangedMethod
	public static class CasePMD_StringMethodNameButNotString {
		private static class LikeString {
			final String string;

			private LikeString(String string) {
				this.string = string;
			}

			// This method has same name than String.contentEquals, but it is not String.contentEquals
			public boolean contentEquals(String otherString) {
				return string.contentEquals(otherString);
			}
		}

		public Object post(LikeString input) {
			return input.contentEquals("hardcoded");
		}
	}

	@CompareMethods
	public static class InLambda {
		public Object pre(List<String> input) {
			return input.stream().filter(s -> s.equals("someString")).findAny();
		}

		public Object post(List<String> input) {
			return input.stream().filter(s -> "someString".equals(s)).findAny();
		}
	}

	@CompareMethods
	public static class ConstantIsStaticField {
		public Object pre(String x) {
			return x.equals(JsonFactory.FORMAT_NAME_JSON);
		}

		public Object post(String x) {
			return JsonFactory.FORMAT_NAME_JSON.equals(x);
		}
	}

	@CompareMethods
	public static class CustomObjectFieldIsStaticField {
		public Object pre(ObjectMapper o) {
			return o.getFactory().getFormatName().equals(JsonFactory.FORMAT_NAME_JSON);
		}

		public Object post(ObjectMapper o) {
			return JsonFactory.FORMAT_NAME_JSON.equals(o.getFactory().getFormatName());
		}
	}

	@UnchangedMethod
	public static class CheckStartsWith {
		public Object post(String o) {
			return o.startsWith(JsonFactory.FORMAT_NAME_JSON);
		}
	}

	// TODO This is a bug as we switch due to the field name mislead supposing it is a constant
	@CompareMethods
	public static class ConstantIsIllNamedObjectField {
		private static class LikeString {
			public final String FORMAT_NAME_JSON = "someString";
		}

		final LikeString o = new LikeString();

		public Object pre(String x) {
			return x.equals(o.FORMAT_NAME_JSON);
		}

		public Object post(String x) {
			return o.FORMAT_NAME_JSON.equals(x);
		}
	}

}
