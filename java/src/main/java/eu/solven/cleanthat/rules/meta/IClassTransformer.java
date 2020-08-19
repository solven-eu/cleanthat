package eu.solven.cleanthat.rules.meta;

import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * For classes knowing how to modify code
 * 
 * @author Benoit Lacelle
 *
 */
public interface IClassTransformer {

	void transform(MethodDeclaration pre);

}
