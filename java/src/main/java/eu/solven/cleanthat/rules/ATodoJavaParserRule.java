package eu.solven.cleanthat.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Helps preparing rules
 *
 * @author Benoit Lacelle
 */
public abstract class ATodoJavaParserRule extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ATodoJavaParserRule.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_LATEST;
	}

	public String sonarUrl() {
		return "?";
	}

	public String pmdUrl() {
		return "?";
	}

	public String jsparrowUrl() {
		return "?";
	}

	@Override
	public boolean transform(MethodDeclaration tree) {
		LOGGER.debug("TODO");
		return false;
	}
}
