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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `!(a == 2)` into `a != 2`
 *
 * @author Benoit Lacelle
 */
public class SimplifyBooleanExpression extends AJavaparserExprMutator {
	final Map<BinaryExpr.Operator, BinaryExpr.Operator> operatorToOpposite =
			ImmutableMap.<BinaryExpr.Operator, BinaryExpr.Operator>builder()
					.put(BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS)
					.put(BinaryExpr.Operator.LESS, BinaryExpr.Operator.GREATER_EQUALS)
					.put(BinaryExpr.Operator.LESS_EQUALS, BinaryExpr.Operator.GREATER)
					.put(BinaryExpr.Operator.GREATER, BinaryExpr.Operator.LESS_EQUALS)
					.put(BinaryExpr.Operator.GREATER_EQUALS, BinaryExpr.Operator.LESS)
					.build();

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("PitFall");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1940");
	}

	@Override
	public Optional<String> getCleanthatId() {
		return Optional.of("SimplifyBooleanExpression");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isUnaryExpr()) {
			return false;
		}
		var unaryExpr = expr.asUnaryExpr();

		if (unaryExpr.getOperator() != UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
			return false;
		}

		// Remove enclosedExpr as they are no-op
		var underlyingExpression = unaryExpr.getExpression();
		while (underlyingExpression.isEnclosedExpr()) {
			underlyingExpression = underlyingExpression.asEnclosedExpr().getInner();
		}

		if (!underlyingExpression.isBinaryExpr()) {
			return false;
		}

		var underlyingBinaryExpr = underlyingExpression.asBinaryExpr();

		Operator initialOperator = underlyingBinaryExpr.getOperator();
		if (!operatorToOpposite.containsKey(initialOperator)) {
			return false;
		}

		boolean replaced = tryReplace(expr, underlyingBinaryExpr);

		if (replaced) {
			// Edit the operator only if the optional node replace succeeded
			underlyingBinaryExpr.setOperator(operatorToOpposite.get(initialOperator));
		}
		return replaced;
	}
}
