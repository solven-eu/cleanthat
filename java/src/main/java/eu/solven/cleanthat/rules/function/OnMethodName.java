package eu.solven.cleanthat.rules.function;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Make easier processing on code-tree
 * 
 * @author Benoit Lacelle
 *
 */
public interface OnMethodName {
	void onMethodName(Node node, Expression scope, ResolvedType type);
}
