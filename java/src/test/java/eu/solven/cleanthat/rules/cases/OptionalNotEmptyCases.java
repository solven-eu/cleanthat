package eu.solven.cleanthat.rules.cases;

import java.util.Map;
import java.util.Optional;

import eu.solven.cleanthat.rules.OptionalNotEmpty;
import eu.solven.cleanthat.rules.cases.annotations.CompareMethods;
import eu.solven.cleanthat.rules.cases.annotations.UnchangedMethod;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;

public class OptionalNotEmptyCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new OptionalNotEmpty();
	}

	@CompareMethods
	public static class CaseNotEmpty {
		public Object pre(Optional<?> input) {
			return !input.isEmpty();
		}

		public Object post(Optional<?> input) {
			return input.isPresent();
		}
	}

	@CompareMethods
	public static class CaseNotPresent {
		public Object pre(Optional<?> input) {
			return !input.isPresent();
		}

		public Object post(Optional<?> input) {
			return input.isEmpty();
		}
	}

	@UnchangedMethod
	public static class CaseOnMap {
		public Object post(Map<?, ?> input) {
			return !input.isEmpty();
		}

		// public Object post(Map<?, ?> input) {
		// return !input.isEmpty();
		// }
	}
}
