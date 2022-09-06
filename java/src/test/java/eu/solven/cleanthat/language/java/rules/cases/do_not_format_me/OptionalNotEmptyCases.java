package eu.solven.cleanthat.language.java.rules.cases.do_not_format_me;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.solven.cleanthat.language.java.rules.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.OptionalNotEmpty;
import eu.solven.cleanthat.language.java.rules.test.ACases;

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
	}

	@UnchangedMethod
	public static class CaseOnHashMap {
		public Object post(HashMap<?, ?> input) {
			return !input.isEmpty();
		}
	}

	@UnchangedMethod
	public static class CaseOnLambda {
		public Object post(List<Map<?, ?>> input) {
			return input.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
		}
	}
}
