package eu.solven.cleanthat.rules.meta;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

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

	default boolean transformMethod(MethodDeclaration pre) {
		return false;
	}

	String minimalJavaVersion();

	default boolean transformType(TypeDeclaration<?> pre) {
		return false;
	}
}
