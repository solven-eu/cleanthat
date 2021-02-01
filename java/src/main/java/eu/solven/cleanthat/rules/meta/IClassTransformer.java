package eu.solven.cleanthat.rules.meta;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * For classes knowing how to modify code
 *
 * @author Benoit Lacelle
 */
public interface IClassTransformer {

	boolean transform(MethodDeclaration pre);

	String minimalJavaVersion();

	default void transformType(TypeDeclaration<?> pre) {
		// No impact over types
	}
}
