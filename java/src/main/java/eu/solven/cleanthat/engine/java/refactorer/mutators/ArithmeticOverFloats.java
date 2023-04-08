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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.OptionalOrRejection;

/**
 * Turns '1F + 0.1F` into `(double) 1F + 0.1F`
 *
 * @author Benoit Lacelle
 */
public class ArithmeticOverFloats extends AJavaparserExprMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArithmeticOverFloats.class);

	private static final Set<BinaryExpr.Operator> MATH_FLOAT_OPERATORS = Set.of(BinaryExpr.Operator.PLUS,
			BinaryExpr.Operator.MINUS,
			BinaryExpr.Operator.MULTIPLY,
			BinaryExpr.Operator.DIVIDE);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2164");
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

		if (!MATH_FLOAT_OPERATORS.contains(binaryExpr.getOperator())) {
			return false;
		} else if (!scopeHasRequiredType(Optional.of(binaryExpr.getLeft()), float.class)) {
			return false;
		} else if (!scopeHasRequiredType(Optional.of(binaryExpr.getRight()), float.class)) {
			return false;
		}

		OptionalOrRejection<Expression> needCastToDouble = optNeedCastToDouble(binaryExpr);
		if (needCastToDouble.isRejected()) {
			return false;
		}

		binaryExpr.setLeft(new CastExpr(new ClassOrInterfaceType("double"), binaryExpr.getLeft()));

		needCastToDouble.ifPresent(e -> {
			Expression casted = e.clone();

			if (!casted.isEnclosedExpr()) {
				casted = new EnclosedExpr(casted);
			}

			e.replace(new CastExpr(new ClassOrInterfaceType("float"), casted));
		});

		return true;
	}

	private OptionalOrRejection<Expression> optNeedCastToDouble(BinaryExpr binaryExpr) {
		Optional<Expression> needCastToDouble = Optional.empty();

		Expression child = binaryExpr;
		while (true) {
			Node parent;

			Optional<Node> optParentNode = child.getParentNode();
			if (optParentNode.isPresent()) {
				parent = optParentNode.get();
			} else {
				break;
			}

			if (parent instanceof BinaryExpr) {
				BinaryExpr parentBinaryExpr = (BinaryExpr) parent;
				if (!MATH_FLOAT_OPERATORS.contains(parentBinaryExpr.getOperator())) {
					// TODO Document this case
					return OptionalOrRejection.reject();
				}
			} else if (parent instanceof MethodCallExpr) {
				// TODO Analyze if the parent accepts a double or a float
				return OptionalOrRejection.reject();
			} else if (parent instanceof EnclosedExpr) {
				LOGGER.debug("{} is a no-op", EnclosedExpr.class);
			} else if (parent instanceof NodeWithType<?, ?>) {
				Type type = ((NodeWithType<?, ?>) parent).getType();

				Optional<ResolvedType> optResolvedType = optResolvedType(type);

				if (optResolvedType.isEmpty()) {
					return OptionalOrRejection.reject();
				} else if (isAssignableBy(Float.class.getName(), optResolvedType.get())) {
					// We cast the double into float as it is the type of the initialized variable
					needCastToDouble = Optional.of(child);
				} else if (isAssignableBy(Number.class.getName(), optResolvedType.get())) {
					// We turned a float into a double, which is finally assigned to a double: fine
					needCastToDouble = Optional.empty();
				} else {
					return OptionalOrRejection.reject();
				}
			} else {
				break;
			}

			if (parent instanceof Expression) {
				child = (Expression) parent;
			} else {
				break;
			}
		}
		return OptionalOrRejection.optional(needCastToDouble);
	}
}
