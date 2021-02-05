package eu.solven.cleanthat.rules.meta;

import com.github.javaparser.ast.Node;

/**
 * For classes knowing how to modify code
 *
 * @author Benoit Lacelle
 */
public interface IClassTransformer {

	// For java, prefer Checkstyle name, else PMD name
	default String getId() {
		return "TODO";
	}

	String minimalJavaVersion();

	/**
	 * 
	 * @param pre
	 * @return true if the AST has been modified.
	 */
	boolean walkNode(Node pre);

}
