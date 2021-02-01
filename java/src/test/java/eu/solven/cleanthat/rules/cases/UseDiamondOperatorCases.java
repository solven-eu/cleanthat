package eu.solven.cleanthat.rules.cases;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.rules.UseDiamondOperator;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;

public class UseDiamondOperatorCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new UseDiamondOperator();
	}

	public static class CaseCollection {
		public String getTitle() {
			return "Map";
		}

		public Map<String, List<String>> pre() {
			return new HashMap<String, List<String>>();
		}

		public Map<String, List<String>> post(Collection<?> input) {
			return new HashMap<>();
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
