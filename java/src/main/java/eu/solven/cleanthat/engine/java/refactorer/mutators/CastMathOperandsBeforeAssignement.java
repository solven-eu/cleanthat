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
import java.util.function.Function;

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
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.BinaryExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ResolvedTypeHelpers;

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
		var leftIsFloating = scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), float.class)
				|| scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), double.class);
		var rightIsFloating = scopeHasRequiredType(Optional.of(binaryExpr.getRight()), float.class)
				|| scopeHasRequiredType(Optional.of(binaryExpr.getRight()), double.class);
		if (leftIsFloating || rightIsFloating) {
			return false;
		}

		if (isUsedAsAIntOrLong(binaryExpr)) {
			// The division is stored as an int/long: no need to divide as floating numbers
			return false;
		}

		Optional<PrimitiveType> optFloatOrDouble = optFloatOrDoubleUsage(binaryExpr);
		if (optFloatOrDouble.isEmpty()) {
			return false;
		}

		Optional<Expression> optIntOrLongLiteral =
				BinaryExprHelpers.findAny(binaryExpr, n -> n.isLiteralStringValueExpr(), true);
		if (optIntOrLongLiteral.isPresent()) {
			// Why casting from int to float instead of double?
			// It depends on the usage of the division (like for short division)
			LiteralStringValueExpr intOrLongLiteralExpr = optIntOrLongLiteral.get().asLiteralStringValueExpr();
			String suffix;

			if (optFloatOrDouble.get().equals(PrimitiveType.floatType())) {
				suffix = "F";
			} else {
				suffix = "D";
			}

			tryReplace(intOrLongLiteralExpr, new DoubleLiteralExpr(intOrLongLiteralExpr.getValue() + suffix));
		} else {
			Expression needToCast = binaryExpr.getLeft();

			optResolvedType(binaryExpr.getLeft());

			CastExpr newLeft = new CastExpr(optFloatOrDouble.get(), needToCast);
			binaryExpr.setLeft(newLeft);
			// Restore the parent as it is removed by `.setLeft(...)`
			needToCast.setParentNode(newLeft);
		}
		return true;
	}

	private boolean onNotDivision(Expression expr, BinaryExpr binaryExpr) {
		if (!scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), int.class)) {
			return false;
		} else if (!scopeHasRequiredType(Optional.of(binaryExpr.getRight()), int.class)) {
			return false;
		}

		if (!isUsedAsALong(expr)) {
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
			tryReplace(intLiteralExpr, new LongLiteralExpr(intLiteralExpr.getValue() + "L"));
		} else {
			binaryExpr.setLeft(new CastExpr(PrimitiveType.longType(), binaryExpr.getLeft()));
		}
		return true;
	}

	/**
	 * 
	 * @param expr
	 * @return true if we know the expr is used as a long. false if used as an int, or if we do not know.
	 */
	private boolean isUsedAsALong(Expression expr) {
		return doMap(expr, optResolvedType -> {
			if (typeWouldNotOverflowFromInt(optResolvedType)) {
				// No need to prevent overflow as an overflow is expected by the returned type
				return Optional.of("");
			} else {
				return Optional.empty();
			}
		}).isPresent();
	}

	private boolean typeWouldNotOverflowFromInt(Optional<ResolvedType> optResolvedType) {
		if (ResolvedTypeHelpers.typeIsAssignable(optResolvedType, int.class.getName())
				|| ResolvedTypeHelpers.typeIsAssignable(optResolvedType, Integer.class.getName())) {
			return false;
		}

		// TODO This looks sub-optimal
		return ResolvedTypeHelpers.typeIsAssignable(optResolvedType, long.class.getName())
				|| ResolvedTypeHelpers.typeIsAssignable(optResolvedType, float.class.getName())
				|| ResolvedTypeHelpers.typeIsAssignable(optResolvedType, double.class.getName())
				|| ResolvedTypeHelpers.typeIsAssignable(optResolvedType, Number.class.getName());
	}

	private boolean isIntOrLong(Optional<ResolvedType> optResolvedType) {
		if (optResolvedType.isEmpty()) {
			return false;
		} else if (optResolvedType.get().isPrimitive()) {
			return ResolvedTypeHelpers.typeIsAssignable(optResolvedType, int.class.getName())
					|| ResolvedTypeHelpers.typeIsAssignable(optResolvedType, long.class.getName());
		} else if (ResolvedTypeHelpers.typeIsAssignable(optResolvedType, Number.class.getName())) {
			// Storing `i1/i2` in a Number is ambiguous. Then we keep the Number as an int/long
			// This would also catch boxed FLoat and Double. However, the code `(Float) (i1 / i2)` is illegal, then it
			// is not a case need to be considered. (i.e. if we encounter this code, it means the input code is illegal)
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @param expr
	 * @return true if we know the expr is used as an int or a long. false if we do not know.
	 */
	private boolean isUsedAsAIntOrLong(Expression expr) {
		return doMap(expr, optResolvedType -> {
			if (isIntOrLong(optResolvedType)) {
				// No need to prevent overflow as an overflow is expected by the returned type
				return Optional.of("");
			} else {
				return Optional.empty();
			}
		}).isPresent();
	}

	/**
	 * 
	 * @param expr
	 * @return true if we know the expr is used as an int or a long. false if we do not know.
	 */
	private Optional<PrimitiveType> optFloatOrDoubleUsage(Expression expr) {
		return doMap(expr, optResolvedType -> {
			if (ResolvedTypeHelpers.typeIsAssignable(optResolvedType, float.class.getName())) {
				return Optional.of(PrimitiveType.floatType());
			} else if (ResolvedTypeHelpers.typeIsAssignable(optResolvedType, double.class.getName())) {
				return Optional.of(PrimitiveType.doubleType());
			} else {
				return Optional.empty();
			}
		});
	}

	private <T> Optional<T> doMap(Expression expr, Function<Optional<ResolvedType>, Optional<T>> mapper) {
		@SuppressWarnings("rawtypes")
		Class[] classes = { AssignExpr.class,
				CastExpr.class,
				LambdaExpr.class,
				ReturnStmt.class,
				MethodCallExpr.class,
				ObjectCreationExpr.class };
		Optional<Node> optAncestorDefiningType = expr.findAncestor(classes);
		if (optAncestorDefiningType.isEmpty()) {
			return Optional.empty();
		} else if (optAncestorDefiningType.get() instanceof NodeWithType) {
			NodeWithType<?, ?> castExpr = (NodeWithType<?, ?>) optAncestorDefiningType.get();
			Optional<ResolvedType> optResolvedType = ResolvedTypeHelpers.optResolvedType(castExpr.getType());

			return mapper.apply(optResolvedType);
		} else if (optAncestorDefiningType.get() instanceof ReturnStmt) {
			ReturnStmt returnStmt = (ReturnStmt) optAncestorDefiningType.get();

			Optional<Node> optParentNodeNotBlock = returnStmt.getParentNode();
			while (optParentNodeNotBlock.isPresent() && optParentNodeNotBlock.get() instanceof BlockStmt) {
				optParentNodeNotBlock = optParentNodeNotBlock.get().getParentNode();
			}

			if (optParentNodeNotBlock.isEmpty()) {
				return Optional.empty();
			} else if (optParentNodeNotBlock.get() instanceof MethodDeclaration) {
				MethodDeclaration methodDecl = (MethodDeclaration) optParentNodeNotBlock.get();

				Optional<ResolvedType> optResolvedType = ResolvedTypeHelpers.optResolvedType(methodDecl.getType());

				return mapper.apply(optResolvedType);
			} else if (optParentNodeNotBlock.get() instanceof LambdaExpr) {
				// TODO Guess the output type from a LambdaExpr
				return Optional.empty();
			} else {
				return Optional.empty();
			}
		}

		// TODO Guess the output type from a MethodCallExpr, ObjectCreationExpr, etc
		return Optional.empty();
	}
}
