package eu.solven.cleanthat.rules;

import eu.solven.cleanthat.rules.framework.ATodoJavaParserRule;
import eu.solven.cleanthat.rules.framework.IJdkVersionConstants;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Use the diamond operation '<>' whenever possible. Some cases are available only since JDK8
 *
 * @author Benoit Lacelle
 */
public class UseDiamondOperatorJdk8 extends ATodoJavaParserRule implements IClassTransformer {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	public String sonarUrl() {
		return "https://rules.sonarsource.com/java/RSPEC-2293";
	}

	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#usediamondoperator";
	}

	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

}
