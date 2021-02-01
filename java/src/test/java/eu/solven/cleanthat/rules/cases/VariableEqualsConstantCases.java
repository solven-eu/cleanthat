package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.rules.VariableEqualsConstant;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;
import eu.solven.cleanthat.rules.test.ICaseOverMethod;

public class VariableEqualsConstantCases extends ACases {
	public IClassTransformer getTransformer() {
		return new VariableEqualsConstant();
	}

	public static class CaseConstantString implements ICaseOverMethod {
		public Object pre(String input) {
			return input.equals("hardcoded");
		}

		public Object post(String input) {
			return "hardcoded".equals(input);
		}
	}

}
