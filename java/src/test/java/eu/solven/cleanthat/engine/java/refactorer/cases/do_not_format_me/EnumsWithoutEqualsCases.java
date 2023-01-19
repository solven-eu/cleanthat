package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.math.RoundingMode;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.mutators.EnumsWithoutEquals;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
public class EnumsWithoutEqualsCases extends ARefactorerCases {
	@Override
	public IClassTransformer getTransformer() {
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
}
