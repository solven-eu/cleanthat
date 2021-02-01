package eu.solven.cleanthat.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Use the diamond operation '<>' whenever possible. Some cases are available only since JDK8
 *
 * @author Benoit Lacelle
 */
public class UseDiamondOperatorJdk8 extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseDiamondOperatorJdk8.class);

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

	@Override
	public boolean transform(MethodDeclaration tree) {
		LOGGER.debug("TODO");
		return false;
	}

}
