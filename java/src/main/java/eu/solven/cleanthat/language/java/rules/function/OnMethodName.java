package eu.solven.cleanthat.language.java.rules.function;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Make easier processing on code-tree
 * 
 * @author Benoit Lacelle
 *
 */
public interface OnMethodName {
	void onMethodName(MethodCallExpr node, Expression scope, ResolvedType type);
}
