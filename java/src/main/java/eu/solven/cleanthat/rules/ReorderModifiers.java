package eu.solven.cleanthat.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Use the diamond operation '<>' whenever possible.
 *
 * @author Benoit Lacelle
 */
public class ReorderModifiers extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReorderModifiers.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/reorder-modifiers.html";
	}

	@Override
	public boolean transform(MethodDeclaration tree) {
		LOGGER.debug("TODO");
		return false;
	}
}
