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

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.google.common.collect.Maps;

/**
 * Helps mutating {@link BinaryExpr}
 * 
 * @author Benoit Lacelle
 *
 */
public class BinaryExprHelpers {
	protected BinaryExprHelpers() {
		// hidden
	}

	@SuppressWarnings("unchecked")
	public static <S extends Expression, T extends Expression> Optional<Map.Entry<S, T>> findPair(BinaryExpr binaryExpr,
			Predicate<Expression> keyPredicate,
			Predicate<Expression> valuePredicate) {
		Expression left = binaryExpr.getLeft();
		Expression right = binaryExpr.getRight();
		if (keyPredicate.test(left) && valuePredicate.test(right)) {
			return Optional.of(Maps.<S, T>immutableEntry((S) left, (T) right));
		} else if (keyPredicate.test(right) && valuePredicate.test(left)) {
			return Optional.of(Maps.<S, T>immutableEntry((S) right, (T) left));
		}

		return Optional.empty();
	}

	public static Optional<Expression> findAny(BinaryExpr binaryExpr,
			Predicate<Expression> operandPredicate,
			boolean cutThroughEnclosedExpr) {
		Expression left = binaryExpr.getLeft();

		if (cutThroughEnclosedExpr) {
			while (left.isEnclosedExpr()) {
				left = left.asEnclosedExpr().getInner();
			}
		}

		if (operandPredicate.test(left)) {
			return Optional.of(left);
		} else {
			Expression right = binaryExpr.getRight();
			if (cutThroughEnclosedExpr) {
				while (right.isEnclosedExpr()) {
					right = right.asEnclosedExpr().getInner();
				}
			}

			if (operandPredicate.test(right)) {
				return Optional.of(right);
			} else {
				return Optional.empty();
			}
		}
	}
}
