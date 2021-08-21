package eu.solven.cleanthat.language.java.rules.cases;

import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.NumberToValueOf;
import eu.solven.cleanthat.language.java.rules.test.ACases;

public class SwitchNumberToValueOfCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new NumberToValueOf();
	}

}
