package eu.solven.cleanthat.rules.cases;

import java.math.RoundingMode;

import eu.solven.cleanthat.rules.EnumsWithoutEquals;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
public class EnumsWithoutEqualsCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new EnumsWithoutEquals();
	}

	public static class EnumInRightHandSide implements ICaseOverMethod {
		public boolean pre(RoundingMode roundingMode) {
			return roundingMode.equals(RoundingMode.UP);
		}

		public boolean post(RoundingMode roundingMode) {
			return roundingMode == RoundingMode.UP;
		}
	}

	public static class CaseEnumInLeftHandSide implements ICaseOverMethod {
		public boolean pre(RoundingMode roundingMode) {
			return RoundingMode.UP.equals(roundingMode);
		}

		public boolean post(RoundingMode roundingMode) {
			return RoundingMode.UP == roundingMode;
		}
	}

	public static class CaseEnumInInfixExpression implements ICaseOverMethod {
		public boolean pre(RoundingMode roundingMode) {
			return !RoundingMode.UP.equals(roundingMode);
		}

		public boolean post(RoundingMode roundingMode) {
			return RoundingMode.UP != roundingMode;
		}
	}
}
