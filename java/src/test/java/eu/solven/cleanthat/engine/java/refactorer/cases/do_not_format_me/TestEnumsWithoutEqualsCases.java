package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.math.RoundingMode;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EnumsWithoutEquals;
import eu.solven.cleanthat.engine.java.refactorer.test.AJavaparserRefactorerCases;

// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
public class TestEnumsWithoutEqualsCases extends AJavaparserRefactorerCases {
	@Override
	public IJavaparserAstMutator getTransformer() {
		return new EnumsWithoutEquals();
	}

	@CompareMethods
	public static class EnumInRightHandSide {
		public boolean pre(RoundingMode roundingMode) {
			return roundingMode.equals(RoundingMode.UP);
		}

		public boolean post(RoundingMode roundingMode) {
			return roundingMode == RoundingMode.UP;
		}
	}

	@CompareMethods
	public static class CaseEnumInLeftHandSide {
		public boolean pre(RoundingMode roundingMode) {
			return RoundingMode.UP.equals(roundingMode);
		}

		public boolean post(RoundingMode roundingMode) {
			return RoundingMode.UP == roundingMode;
		}
	}

	@CompareMethods
	public static class CaseEnumInInfixExpression {
		public boolean pre(RoundingMode roundingMode) {
			return !RoundingMode.UP.equals(roundingMode);
		}

		public boolean post(RoundingMode roundingMode) {
			return RoundingMode.UP != roundingMode;
		}
	}

	@UnmodifiedCompilationUnitAsString(pre = "package custom.project;\n"
			+ "\n"
			+ "import custom.library.RoundingMode;\n"
			+ "\n"
			+ "public class CheckEnum {\n"
			+ "	public boolean pre(RoundingMode roundingMode) {\n"
			+ "		return roundingMode.equals(RoundingMode.UP);\n"
			+ "	}\n"
			+ "}\n"
			+ "")
	public static class UnknownType {
	}
}
