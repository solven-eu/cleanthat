package eu.solven.cleanthat.rules;

import java.math.RoundingMode;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
public class EnumsWithoutEqualsCases {
	public String getId() {
		return "EnumsWithoutEquals";
	}

	public IClassTransformer getTransformer() {
		return new EnumsWithoutEquals();
	}

	public static class Case0 {
		public String getTitle() {
			return "Enum in right-hand-side";
		}

		public boolean pre(RoundingMode roundingMode) {
			return roundingMode.equals(RoundingMode.UP);
		}

		public boolean post(RoundingMode roundingMode) {
			return roundingMode == RoundingMode.UP;
		}
	}

	public static class Case1 {
		public String getTitle() {
			return "Enum in left-hand-side";
		}

		public boolean pre(RoundingMode roundingMode) {
			return RoundingMode.UP.equals(roundingMode);
		}

		public boolean post(RoundingMode roundingMode) {
			return RoundingMode.UP == roundingMode;
		}
	}

	public static class Case2 {
		public String getTitle() {
			return "Enum in infix expression";
		}

		public boolean pre(RoundingMode roundingMode) {
			return !RoundingMode.UP.equals(roundingMode);
		}

		public boolean post(RoundingMode roundingMode) {
			return !(RoundingMode.UP == roundingMode);
		}
	}
}
