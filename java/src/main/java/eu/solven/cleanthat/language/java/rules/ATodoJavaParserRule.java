package eu.solven.cleanthat.language.java.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;

/**
 * Helps preparing rules
 *
 * @author Benoit Lacelle
 */
public abstract class ATodoJavaParserRule extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ATodoJavaParserRule.class);

	@Override
	public boolean isProductionReady() {
		return false;
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_LATEST;
	}

	@Override
	public boolean walkNode(Node tree) {
		LOGGER.debug("TODO");
		return false;
	}
}
