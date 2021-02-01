package eu.solven.cleanthat.rules.cases;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.rules.UseDiamondOperator;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

public class UseDiamondOperatorCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new UseDiamondOperator();
	}

	public static class CaseCollection implements ICaseOverMethod {
		public Map<String, List<String>> pre() {
			return new HashMap<String, List<String>>();
		}

		public Map<String, List<String>> post(Collection<?> input) {
			return new HashMap<>();
		}
	}

}
