package eu.solven.cleanthat.language.java.rules;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.meta.IRuleExternalUrls;

/**
 * Order modifiers according the the Java specification.
 *
 * @author Benoit Lacelle
 */
public class NoOpJavaParserRule extends AJavaParserRule implements IClassTransformer, IRuleExternalUrls {
	@Override
	public String getId() {
		return "NoOp";
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		// We return true to indicate we did modify the node, even through this is a no-op operator
		return true;
	}
}
