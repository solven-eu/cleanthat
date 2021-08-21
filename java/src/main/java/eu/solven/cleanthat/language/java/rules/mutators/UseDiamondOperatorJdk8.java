package eu.solven.cleanthat.language.java.rules.mutators;

import java.util.Optional;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.rules.ATodoJavaParserRule;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;

/**
 * Use the diamond operation {@code <>} whenever possible. Some cases are available only since JDK8
 *
 * @author Benoit Lacelle
 */
@Deprecated(since = "Not-ready")
public class UseDiamondOperatorJdk8 extends ATodoJavaParserRule implements IClassTransformer {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
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
	public Optional<String> getPmdId() {
		return Optional.of("UseDiamondOperator");
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

}
