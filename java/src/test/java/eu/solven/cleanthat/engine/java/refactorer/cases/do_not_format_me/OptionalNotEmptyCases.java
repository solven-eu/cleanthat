package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnchangedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.mutators.OptionalNotEmpty;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;

public class OptionalNotEmptyCases extends ARefactorerCases {
	@Override
	public IMutator getTransformer() {
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
		public Object pre(Map<?, ?> input) {
			return !input.isEmpty();
		}
	}

	@UnchangedMethod
	public static class CaseOnHashMap {
		public Object pre(HashMap<?, ?> input) {
			return !input.isEmpty();
		}
	}

	@UnchangedMethod
	public static class CaseOnLambda {
		public Object pre(List<Map<?, ?>> input) {
			return input.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
		}
	}
}
