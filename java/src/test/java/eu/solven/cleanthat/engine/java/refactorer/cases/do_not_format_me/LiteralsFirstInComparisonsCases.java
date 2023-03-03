package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.junit.Ignore;
import org.springframework.core.io.InputStreamResource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.LiteralsFirstInComparisons;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

public class LiteralsFirstInComparisonsCases extends AJavaparserRefactorerCases {

	@Override
	public IJavaparserMutator getTransformer() {
		return new LiteralsFirstInComparisons();
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

	@UnmodifiedMethod
	public static class CaseConstantStringWithAnotherConstant {
		public Object pre() {
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

	@UnmodifiedMethod
	public static class CaseConstantCustom_constructor_bothsides {
		private static final class SomeClass {

		}

		private static final class OtherClass {

		}

		@SuppressWarnings("unlikely-arg-type")
		public Object pre(Object input) {
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

	@UnmodifiedMethod
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

		public Object pre(LikeString input) {
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

	@UnmodifiedMethod
	public static class CheckStartsWith_ConstantAfter {
		public Object pre(String o) {
			return o.startsWith(JsonFactory.FORMAT_NAME_JSON);
		}
	}

	@UnmodifiedMethod
	public static class ConstantIsIllNamedObjectField {
		private static class LikeString {
			public final String FORMAT_NAME_JSON = "someString";
		}

		final LikeString o = new LikeString();

		public Object pre(String x) {
			return x.equals(o.FORMAT_NAME_JSON);
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

	public class MultipartInputStreamFileResource extends InputStreamResource {

		private final String filename;

		public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
			super(inputStream);
			this.filename = filename;
		}

		@Override
		public String getFilename() {
			return this.filename;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(filename);
			return result;
		}

	}

	@UnmodifiedMethod
	public static class CallsSuperEquals {

		public Object pre(CallsSuperEquals o) {
			if (this == o) {
				return true;
			}
			if (!super.equals(o)) {
				return false;
			}
			if (getClass() != o.getClass()) {
				return false;
			}
			return true;
		}
	}

	@UnmodifiedMethod
	public static class TwoUnknownVariable {

		public Object pre(String o1, String o2) {
			return o1.equals(o2);
		}
	}

	@UnmodifiedMethod
	public static class CompareVariableWithMethodCall {
		public Object pre(String input, Object object) {
			return input.equals(object.toString());
		}
	}

	@UnmodifiedMethod
	public static class CompareMethodCallWithVariable {
		public Object pre(String input, Object object) {
			return object.toString().equals(input);
		}
	}

	private static final String EOL = System.lineSeparator();

	@UnmodifiedMethod
	public static class HardcodedStringVsStatic {
		public Object pre() {
			return "\r\n".equals(EOL);
		}
	}

	// @CompareCompilationUnitsAsResources(pre =
	// "/source/do_not_format_me/LiteralsFirstInComparisons/TestApexCubeSnapshooterOnUpdatedCountries.java",
	// post = "/source/do_not_format_me/LiteralsFirstInComparisons/TestApexCubeSnapshooterOnUpdatedCountries.java")
	// public static class Issue_resolvingType {
	// }

	@UnmodifiedCompilationUnitAsString(pre = "package blasd.apex.server.cube.snapshot;\n" + "\n"
			+ "import com.quartetfs.biz.pivot.cube.hierarchy.ILevelInfo;\n"
			+ "\n"
			+ "public class TestApexCubeSnapshooterOnUpdatedCountries {\n"
			+ "\n"
			+ "	public void testQueryOnUpdatePartitions(ILevelInfo countryLevel) {\n"
			+ "		return new AApexCubeSnapshooter() {\n"
			+ "\n"
			+ "				@Override\n"
			+ "				protected String getTargetColumnName(ILevelInfo levelInfo) {\n"
			+ "					if (levelInfo.equals(countryLevel)) {\n"
			+ "						return COUNTRY;\n"
			+ "					} else {\n"
			+ "						return super.getTargetColumnName(levelInfo);\n"
			+ "					}\n"
			+ "				}\n"
			+ "\n"
			+ "			};\n"
			+ "	}\n"
			+ "}\n"
			+ "")
	public static class Issue_unknownType {
	}

}
