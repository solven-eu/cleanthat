package eu.solven.cleanthat.language.java.rules.cases.do_not_format_me;

import java.time.LocalDate;
import java.util.List;

import org.junit.Ignore;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.language.java.rules.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.VariableEqualsConstant;
import eu.solven.cleanthat.language.java.rules.test.ACases;

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

	@UnchangedMethod
	public static class CaseConstantCustom_constructor_bothsides {
		private static final class SomeClass {

		}

		private static final class OtherClass {

		}

		@SuppressWarnings("unlikely-arg-type")
		public Object post(Object input) {
			return new OtherClass().equals(new SomeClass());
		}
	}

	// This is a bug as we are putting null a scope of a method call
	@CompareMethods
	public static class CaseConstantCustom_ref_null {
		private static final SomeClass constantSomeClass = null;

		private static final class SomeClass {

		}

		public Object pre(Object input) {
			return input.equals(constantSomeClass);
		}

		public Object post(Object input) {
			return constantSomeClass.equals(input);
		}
	}

	@CompareMethods
	public static class CaseConstantCustom_ref {
		private static final SomeClass constantSomeClass = new SomeClass();

		private static final class SomeClass {

		}

		public Object pre(Object input) {
			return input.equals(constantSomeClass);
		}

		public Object post(Object input) {
			return constantSomeClass.equals(input);
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

	@CompareMethods
	public static class ConstantAsStatic {
		private static final LocalDate TODAY = LocalDate.now();

		public Object pre(Object x) {
			return x.equals(TODAY);
		}

		public Object post(Object x) {
			return TODAY.equals(x);
		}
	}

}
