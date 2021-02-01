package eu.solven.cleanthat.rules.cases;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.solven.cleanthat.rules.SwitchVariableEqualsConstant;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ACases;

public class SwitchVariableEqualsConstantCases extends ACases {
	public IClassTransformer getTransformer() {
		return new SwitchVariableEqualsConstant();
	}

}
