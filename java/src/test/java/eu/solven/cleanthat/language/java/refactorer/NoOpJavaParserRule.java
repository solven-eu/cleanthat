package eu.solven.cleanthat.language.java.refactorer;

import com.github.javaparser.ast.Node;

import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.meta.IRuleExternalUrls;
import eu.solven.cleanthat.language.java.rules.AJavaParserRule;

/**
 * This {@link AJavaParserRule} does not modify the AST, but always report it as changed. It can be useful to checkthe
 * default behavior of JavaParser.
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
