package eu.solven.cleanthat.language.java.rules.cases;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.language.java.rules.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.UseDiamondOperator;
import eu.solven.cleanthat.language.java.rules.test.ACases;

public class UseDiamondOperatorCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new UseDiamondOperator();
	}

	@CompareMethods
	public static class CaseCollection {
		public Map<String, List<String>> pre() {
			return new HashMap<String, List<String>>();
		}

		public Map<String, List<String>> post() {
			return new HashMap<>();
		}
	}

	@UnchangedMethod
	public static class CaseAnonymousClass {

		public Map<String, List<String>> post() {
			return new HashMap<String, List<String>>() {
				private static final long serialVersionUID = 1L;

				{
					this.put("k", List.of());
				}
			};
		}
	}

}
