package eu.solven.cleanthat.rules.cases;

import eu.solven.cleanthat.rules.SwitchNumberToValueOf;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;

public class SwitchNumberToValueOfCases extends ACases {
	public IClassTransformer getTransformer() {
		return new SwitchNumberToValueOf();
	}

}
