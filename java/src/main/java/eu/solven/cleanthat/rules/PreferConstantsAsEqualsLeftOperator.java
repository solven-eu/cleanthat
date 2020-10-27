package eu.solven.cleanthat.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 *
 * @author Benoit Lacelle
 */
public class PreferConstantsAsEqualsLeftOperator implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public boolean transform(MethodDeclaration pre) {
		LOGGER.debug("TODO");
		return false;
	}
}
