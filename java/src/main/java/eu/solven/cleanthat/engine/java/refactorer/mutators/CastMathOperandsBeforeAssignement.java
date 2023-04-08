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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.BinaryExprHelpers;

/**
 * Turns 'float f = 2/3;` into `float f = (float)2/3;`
 *
 * @author Benoit Lacelle
 */
public class CastMathOperandsBeforeAssignement extends AJavaparserExprMutator {
	private static final Set<BinaryExpr.Operator> MATH_NOT_DIVIDE_OPERATORS =
			Set.of(BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MINUS, BinaryExpr.Operator.MULTIPLY);

	private static final Set<BinaryExpr.Operator> MATH_DIVIDE_OPERATORS = Set.of(BinaryExpr.Operator.DIVIDE);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2184");
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isBinaryExpr()) {
			return false;
		}
		var binaryExpr = expr.asBinaryExpr();

		Operator operator = binaryExpr.getOperator();
		if (MATH_NOT_DIVIDE_OPERATORS.contains(operator)) {
			return onNotDivision(expr, binaryExpr);

		} else if (MATH_DIVIDE_OPERATORS.contains(operator)) {
			return onDivision(binaryExpr);

		}
		return false;
	}

	private boolean onDivision(BinaryExpr binaryExpr) {
		if (!scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), int.class)
				&& !scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), long.class)) {
			return false;
		} else if (!scopeHasRequiredType(Optional.of(binaryExpr.getRight()), int.class)
				&& !scopeHasRequiredType(Optional.of(binaryExpr.getRight()), long.class)) {
			return false;
		}

		Optional<Expression> optIntegerLiteral =
				BinaryExprHelpers.findAny(binaryExpr, n -> n.isIntegerLiteralExpr(), true);
		if (optIntegerLiteral.isPresent()) {
			IntegerLiteralExpr intLiteralExpr = optIntegerLiteral.get().asIntegerLiteralExpr();
			intLiteralExpr.replace(new DoubleLiteralExpr(intLiteralExpr.getValue() + "F"));
		} else {
			Optional<Expression> optLongLiteral =
					BinaryExprHelpers.findAny(binaryExpr, n -> n.isLongLiteralExpr(), true);
			if (optLongLiteral.isPresent()) {
				LongLiteralExpr longLiteralExpr = optLongLiteral.get().asLongLiteralExpr();
				longLiteralExpr.replace(new DoubleLiteralExpr(longLiteralExpr.getValue() + "D"));
			} else {
				binaryExpr.setLeft(new CastExpr(new ClassOrInterfaceType("double"), binaryExpr.getLeft()));
			}
		}
		return true;
	}

	private boolean onNotDivision(Expression expr, BinaryExpr binaryExpr) {
		if (!scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), int.class)) {
			return false;
		} else if (!scopeHasRequiredType(Optional.of(binaryExpr.getRight()), int.class)) {
			return false;
		}

		if (!isUsedAnInt(expr)) {
			return false;
		}

		Optional<Expression> optBinaryExpr = BinaryExprHelpers.findAny(binaryExpr, n -> n.isBinaryExpr(), true);
		if (optBinaryExpr.isPresent()) {
			// Search deeper, as the cast should be done as deeply as possible
			return processNotRecursively(optBinaryExpr.get());
		}

		Optional<Expression> optIntegerLiteral =
				BinaryExprHelpers.findAny(binaryExpr, n -> n.isIntegerLiteralExpr(), true);

		if (optIntegerLiteral.isPresent()) {
			IntegerLiteralExpr intLiteralExpr = optIntegerLiteral.get().asIntegerLiteralExpr();
			intLiteralExpr.replace(new LongLiteralExpr(intLiteralExpr.getValue() + "L"));
		} else {
			binaryExpr.setLeft(new CastExpr(new ClassOrInterfaceType("long"), binaryExpr.getLeft()));
		}
		return true;
	}

	private boolean isUsedAnInt(Expression expr) {
		Class[] classes =
				{ AssignExpr.class, CastExpr.class, LambdaExpr.class, ReturnStmt.class, MethodCallExpr.class };
		Optional<Node> optAncestorDefiningType = expr.findAncestor(classes);
		if (optAncestorDefiningType.isEmpty()) {
			return false;
		} else if (optAncestorDefiningType.get() instanceof NodeWithType) {
			NodeWithType<?, ?> castExpr = (NodeWithType<?, ?>) optAncestorDefiningType.get();
			Optional<ResolvedType> optResolvedType = optResolvedType(castExpr.getType());

			if (typeHasRequiredType(optResolvedType, int.class.getName())
					|| typeHasRequiredType(optResolvedType, Integer.class.getName())) {
				// No need to prevent overflow as an overflow is expected by the returned type
				return false;
			}
		} else if (optAncestorDefiningType.get() instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) optAncestorDefiningType.get();

			Optional<Node> optParentNodeNotBlock = returnStmt.getParentNode();
			while (optParentNodeNotBlock.isPresent() && optParentNodeNotBlock.get() instanceof BlockStmt) {
				optParentNodeNotBlock = optParentNodeNotBlock.get().getParentNode();
			}

			if (optParentNodeNotBlock.isEmpty()) {
				return false;
			} else if (optParentNodeNotBlock.get() instanceof MethodDeclaration) {
				MethodDeclaration methodDecl = (MethodDeclaration) optParentNodeNotBlock.get();

				Optional<ResolvedType> optResolvedType = optResolvedType(methodDecl.getType());

				if (typeHasRequiredType(optResolvedType, int.class.getName())
						|| typeHasRequiredType(optResolvedType, Integer.class.getName())) {
					// No need to prevent overflow as an overflow is expected by the returned type
					return false;
				}
			} else if (optParentNodeNotBlock.get() instanceof LambdaExpr) {
				// TODO Guess the output type from a LambdaExpr
				return false;
			} else {
				return false;
			}
		}

		return true;
	}
}
