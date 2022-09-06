package eu.solven.cleanthat.language.java.rules.cases.do_not_format_me;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.language.java.rules.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.UseIsEmptyOnCollections;
import eu.solven.cleanthat.language.java.rules.test.ACases;

public class UseIsEmptyOnCollectionsCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new UseIsEmptyOnCollections();
	}

	@CompareMethods
	public static class CaseCollection {
		public Object pre(Collection<?> input) {
			return input.size() == 0;
		}

		public Object post(Collection<?> input) {
			return input.isEmpty();
		}
	}

	// Check we handle most Collection sub-types
	@CompareMethods
	public static class CaseList {
		public Object pre(List<?> input) {
			return input.size() == 0;
		}

		public Object post(List<?> input) {
			return input.isEmpty();
		}
	}

	@CompareMethods
	public static class CaseMap {
		public Object pre(Map<?, ?> input) {
			return input.size() == 0;
		}

		public Object post(Map<?, ?> input) {
			return input.isEmpty();
		}
	}

	@CompareMethods
	public static class CaseString {
		public Object pre(String input) {
			return input.length() == 0;
		}

		public Object post(String input) {
			return input.isEmpty();
		}
	}

	// TODO
	// @CompareMethods
	public static class CaseCollection_NotEmpty {
		public Object pre(Collection<?> input) {
			return input.size() != 0;
		}

		public Object post(Collection<?> input) {
			return !input.isEmpty();
		}
	}
}
