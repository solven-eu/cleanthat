/* (C)2023 */
package eu.solven.cleanthat.language.java.refactorer;

public class TestDsl {
	String renameVariable = "VariableDeclarationExpr.name=titi->VariableDeclarationExpr.name.set(var.name + '2')";
	String streamFilterFindAnyIsPresent = "node.instanceof=MethodCallExpr && node.MethodCallExpr.name=isPresent"

			+ "&& node.scope.instanceof=MethodCallExpr && node.scope.MethodCallExpr.name=findAny"
			+ "&& node.scope.scope.instanceof=MethodCallExpr && node.scope.scope.MethodCallExpr.name=isPresent"

			+ "&& node.scope.scope.resolveType=java.util.stream.Stream"

			+ "-> node.replace(MethodCallExpr(node.scope.scope, 'anyMatch', node.scope.arguments))";
}
