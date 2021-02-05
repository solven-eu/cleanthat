package eu.solven.cleanthat.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prevent relying .equals on {@link Enum} types
 *
 * @author Benoit Lacelle
 */
// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
@Deprecated(since = "Not-Ready: how can we infer a Type is an Enum?")
public class EnumsWithoutEquals extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnumsWithoutEquals.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
	}

	// https://stackoverflow.com/questions/55309460/how-to-replace-expression-by-string-in-javaparser-ast
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		onMethodName(node, "equals", (methodNode, scope, type) -> {
			if (type.isReferenceType()) {
				LOGGER.debug("scope={} type={}", scope, type);
			}
		});

		return false;
	}

}
