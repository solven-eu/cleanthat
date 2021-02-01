package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.rules.PrimitiveBoxedForString;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class ReplaceOptionalNotEmptyCases {
	public String getId() {
		return "PrimitiveBoxedForString";
	}

	public IClassTransformer getTransformer() {
		return new PrimitiveBoxedForString();
	}

	public static class CaseNotEmpty {
		public String getTitle() {
			return "someString.equals('hardcoded')";
		}

		public Object pre(String input) {
			return input.equals("hardcoded");
		}

		public Object post(String input) {
			return "hardcoded".equals(input);
		}
	}
}
