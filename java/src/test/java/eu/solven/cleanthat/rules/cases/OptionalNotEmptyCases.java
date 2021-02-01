package eu.solven.cleanthat.rules.cases;

import java.util.Optional;

import eu.solven.cleanthat.rules.OptionalNotEmpty;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

public class OptionalNotEmptyCases implements ICaseOverMethod {
	public IClassTransformer getTransformer() {
		return new OptionalNotEmpty();
	}

	public static class CaseNotEmpty implements ICaseOverMethod {
		public Object pre(Optional<?> input) {
			return !input.isEmpty();
		}

		public Object post(Optional<?> input) {
			return input.isPresent();
		}
	}

	public static class CaseNotPresent implements ICaseOverMethod {
		public Object pre(Optional<?> input) {
			return !input.isPresent();
		}

		public Object post(Optional<?> input) {
			return input.isEmpty();
		}
	}
}
