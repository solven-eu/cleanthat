/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithThrownExceptions;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Helps crafting {@link LambdaExpr}
 * 
 * @author Benoit Lacelle
 *
 */
public class LambdaExprHelpers {
	protected LambdaExprHelpers() {
		// hidden
	}

	public static Optional<LambdaExpr> makeLambdaExpr(SimpleName simpleName, Statement statement) {
		if (statement.isBlockStmt()) {
			return makeLambdaExpr(simpleName, statement.asBlockStmt());
		} else if (statement.isExpressionStmt()) {
			return makeLambdaExpr(simpleName, statement.asExpressionStmt().getExpression());
		} else {
			return Optional.empty();
		}
	}

	public static Optional<LambdaExpr> makeLambdaExpr(SimpleName simpleName, BlockStmt statement) {
		if (!canBePushedInLambdaExpr(statement)) {
			// We can not move AssignExpr inside a LambdaExpr
			return Optional.empty();
		}

		var parameter = new Parameter(new UnknownType(), simpleName);
		return Optional.of(new LambdaExpr(parameter, statement.asBlockStmt()));
	}

	public static Optional<LambdaExpr> makeLambdaExpr(SimpleName simpleName, Expression expression) {
		if (!canBePushedInLambdaExpr(expression)) {
			// We can not move AssignExpr inside a LambdaExpr
			return Optional.empty();
		}

		var parameter = new Parameter(new UnknownType(), simpleName);
		return Optional.of(new LambdaExpr(parameter, expression));
	}

	/**
	 * 
	 * @param node
	 * @return true if this node can be moved inside a {@link LambdaExpr}
	 */
	private static boolean canBePushedInLambdaExpr(Node node) {
		if (hasOuterAssignExpr(node)) {
			return false;
		} else if (node.findFirst(ReturnStmt.class).isPresent()) {
			// Can can not move the `return` inside a LambdaExpr
			return false;
		} else if (node.findFirst(ContinueStmt.class).isPresent()) {
			// TODO We would need to turn `continue;` into `return;`
			// BEWARE of `continue: outerLoop;`
			return false;
		} else if (node.findFirst(BreakStmt.class).isPresent()) {
			return false;
		}

		if (nodeThrowsExplicitException(node)) {
			return false;
		}

		return true;
	}

	/**
	 * Given we do not have the whole classpath, it is difficult to analyze the whole input node. Instead, we will
	 * analyze the first ancestor of type {@link NodeWithThrownExceptions}, and check the returned exceptions.
	 * 
	 * @param node
	 * @return
	 */
	private static boolean nodeThrowsExplicitException(Node node) {
		@SuppressWarnings("rawtypes")
		Optional<NodeWithThrownExceptions> optFirstAncestorWithExceptions =
				node.findAncestor(NodeWithThrownExceptions.class);

		if (optFirstAncestorWithExceptions.isEmpty()) {
			// What does it mean ? In most cases, we are supposed to be wrapped in a MethodCallExpr
			return false;
		}

		@SuppressWarnings("unchecked")
		Optional<?> firstUnknownOrExplicitException = optFirstAncestorWithExceptions.get()
				.getThrownExceptions()
				.stream()
				.filter(Type.class::isInstance)
				.map(t -> (Type) t)
				.filter(t -> {
					Optional<ResolvedType> optResolved = ResolvedTypeHelpers.optResolvedType((Type) t);
					if (optResolved.isEmpty()) {
						return true;
					} else if (ResolvedTypeHelpers.typeIsAssignable(optResolved, RuntimeException.class.getName())) {
						return false;
					} else if (ResolvedTypeHelpers.typeIsAssignable(optResolved, Error.class.getName())) {
						return false;
					}
					// A Throwable which is neither a RuntimeException nor an Error is an explicit Exception
					return true;
				})
				.findFirst();

		return firstUnknownOrExplicitException.isPresent();
	}

	public static boolean hasOuterAssignExpr(Node node) {
		Optional<AssignExpr> optOuterAssignExpr = node.findFirst(AssignExpr.class, assignExpr -> {
			Expression assigned = assignExpr.getTarget();

			return node
					.findFirst(VariableDeclarationExpr.class,
							variableDeclExpr -> variableDeclExpr.getVariables()
									.stream()
									.filter(declared -> declared.getNameAsExpression().equals(assigned))
									.findAny()
									.isPresent())
					.isEmpty();
		});
		if (optOuterAssignExpr.isPresent()) {
			return true;
		}

		Optional<UnaryExpr> optOuterUnaryExpr = node.findFirst(UnaryExpr.class, unaryExpr -> {
			if (unaryExpr.getOperator() != UnaryExpr.Operator.POSTFIX_DECREMENT
					&& unaryExpr.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT
					&& unaryExpr.getOperator() != UnaryExpr.Operator.PREFIX_DECREMENT
					&& unaryExpr.getOperator() != UnaryExpr.Operator.PREFIX_INCREMENT) {
				// Others operator are not modifying the variable
				return false;
			}

			Expression assigned = unaryExpr.getExpression();
			return node
					.findFirst(VariableDeclarationExpr.class,
							variableDeclExpr -> variableDeclExpr.getVariables()
									.stream()
									.filter(declared -> declared.getNameAsExpression().equals(assigned))
									.findAny()
									.isPresent())
					.isEmpty();

		});
		if (optOuterUnaryExpr.isPresent()) {
			return true;
		}

		return false;
	}

	public static boolean changeName(LambdaExpr mapLambdaExpr, SimpleName newName) {
		// The following is broken: it will happens the variable name to the old one, instead of replacing it
		// mapLambdaExpr.getParameter(0).setName();

		if (mapLambdaExpr.getParameters().size() != 1) {
			return false;
		}

		var parameter = new Parameter(new UnknownType(), newName);
		LambdaExpr newlambda = new LambdaExpr(new NodeList<>(parameter),
				mapLambdaExpr.getBody(),
				mapLambdaExpr.isEnclosingParameters());
		mapLambdaExpr.replace(newlambda);

		return true;
	}
}
