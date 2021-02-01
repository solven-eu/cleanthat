package eu.solven.cleanthat.rules.cases;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.rules.UseIsEmptyOnCollections;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class UseIsEmptyOnCollectionsCases {
	public String getId() {
		return "UseIsEmptyOnCollections";
	}

	public IClassTransformer getTransformer() {
		return new UseIsEmptyOnCollections();
	}

	public static class CaseCollection {
		public String getTitle() {
			return "Collection";
		}

		public Object pre(Collection<?> input) {
			return input.size() == 0;
		}

		public Object post(Collection<?> input) {
			return input.isEmpty();
		}
	}

	// Check we handle most Collection sub-types
	public static class CaseList {
		public String getTitle() {
			return "List";
		}

		public Object pre(List<?> input) {
			return input.size() == 0;
		}

		public Object post(List<?> input) {
			return input.isEmpty();
		}
	}

	public static class CaseMap {
		public String getTitle() {
			return "Map";
		}

		public Object pre(Map<?, ?> input) {
			return input.size() == 0;
		}

		public Object post(Map<?, ?> input) {
			return input.isEmpty();
		}
	}

	public static class CaseString {
		public String getTitle() {
			return "String";
		}

		public Object pre(String input) {
			return input.length() == 0;
		}

		public Object post(String input) {
			return input.isEmpty();
		}
	}
}
