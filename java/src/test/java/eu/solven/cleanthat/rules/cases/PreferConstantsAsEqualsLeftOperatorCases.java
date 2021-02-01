package eu.solven.cleanthat.rules.cases;

import java.util.Optional;

import eu.solven.cleanthat.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class PreferConstantsAsEqualsLeftOperatorCases {
	public String getId() {
		return "PrimitiveBoxedForString";
	}

	public IClassTransformer getTransformer() {
		return new PrimitiveBoxedForString();
	}

	public static class CaseNotEmpty {
		public String getTitle() {
			return "!Optional.empty()";
		}

		public Object pre(Optional<?> input) {
			return !input.isEmpty();
		}

		public Object post(Optional<?> input) {
			return input.isPresent();
		}
	}

	public static class CaseNotPresent {
		public String getTitle() {
			return "!Optional.present()";
		}

		public Object pre(Optional<?> input) {
			return !input.isPresent();
		}

		public Object post(Optional<?> input) {
			return input.isEmpty();
		}
	}
}
