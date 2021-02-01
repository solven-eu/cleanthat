package eu.solven.cleanthat.rules;

import eu.solven.cleanthat.rules.meta.IRuleDescriber;

/**
 * Switch o.equals("someString") to "someString".equals(o)
 *
 * @author Benoit Lacelle
 */
public class SwitchVariableEqualsConstant extends ATodoJavaParserRule implements IRuleDescriber {

	@Override
	public boolean isPreventingExceptions() {
		return true;
	}
}
