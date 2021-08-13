package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.language.java.rules.NumberToValueOf;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;

public class SwitchNumberToValueOfCases extends ACases {
	@Override
	public IClassTransformer getTransformer() {
		return new NumberToValueOf();
	}

}
