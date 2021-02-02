package eu.solven.cleanthat.rules;

import eu.solven.cleanthat.rules.framework.ATodoJavaParserRule;
import eu.solven.cleanthat.rules.framework.IJdkVersionConstants;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.meta.IRuleExternalUrls;

/**
 * Use the diamond operation '<>' whenever possible.
 *
 * @author Benoit Lacelle
 */
public class UseDiamondOperator extends ATodoJavaParserRule implements IClassTransformer, IRuleExternalUrls {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public String sonarUrl() {
		return "https://rules.sonarsource.com/java/RSPEC-2293";
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#usediamondoperator";
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

}
