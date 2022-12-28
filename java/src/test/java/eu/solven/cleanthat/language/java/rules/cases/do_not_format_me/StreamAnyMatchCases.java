package eu.solven.cleanthat.language.java.rules.cases.do_not_format_me;

import java.util.List;

import eu.solven.cleanthat.language.java.rules.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.StreamAnyMatch;
import eu.solven.cleanthat.language.java.rules.test.ACases;

public class StreamAnyMatchCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new StreamAnyMatch();
	}

	@CompareMethods
	public static class CaseFindAnyIsPresent {
		public Object pre(List<?> input) {
			return input.stream().filter(o -> null != o).findAny().isPresent();
		}

		public Object post(List<?> input) {
			return input.stream().anyMatch(o -> null != o);
		}
	}

}
