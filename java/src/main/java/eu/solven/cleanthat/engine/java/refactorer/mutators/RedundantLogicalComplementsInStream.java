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
import java.util.stream.Stream;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr.Operator;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `!strings.stream().anyMatch(p)` into `strings.stream().noneMatch(p)`
 *
 * @author Benoit Lacelle
 */
public class RedundantLogicalComplementsInStream extends AJavaparserExprMutator {
	static final String ANY_MATCH = "anyMatch";

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-S4034");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("RedundantLogicalComplementsInStream");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html";
	}

	@SuppressWarnings("PMD.NPathComplexity")
	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}

		var methodCall = expr.asMethodCallExpr();

		if (!"anyMatch".equals(methodCall.getNameAsString()) || methodCall.getArguments().size() != 1) {
			return false;
		}
		if (!scopeHasRequiredType(methodCall.getScope(), Stream.class)) {
			return false;
		}

		var nearestMethodCallAncestor = methodCall;
		while (nearestMethodCallAncestor.getParentNode().isPresent()
				&& nearestMethodCallAncestor.getParentNode().get() instanceof MethodCallExpr) {
			nearestMethodCallAncestor = (MethodCallExpr) nearestMethodCallAncestor.getParentNode().get();
		}

		if (nearestMethodCallAncestor.getParentNode().isEmpty()
				|| !(nearestMethodCallAncestor.getParentNode().get() instanceof UnaryExpr)) {
			return false;
		}

		var unaryExpr = (UnaryExpr) nearestMethodCallAncestor.getParentNode().get();
		if (unaryExpr.getOperator() != Operator.LOGICAL_COMPLEMENT) {
			return false;
		}

		if (!tryReplace(unaryExpr, nearestMethodCallAncestor)) {
			return false;
		}

		var firstArgument = methodCall.getArgument(0);
		Optional<Expression> optNegatedPredicate = searchNegatingPredicate(firstArgument);

		if (optNegatedPredicate.isPresent() && optNegatedPredicate.get().getParentNode().isPresent()) {
			var negatedPredicate = optNegatedPredicate.get();
			if (tryReplace(negatedPredicate.getParentNode().get(), negatedPredicate)) {
				methodCall.setName("allMatch");
			} else {
				methodCall.setName("noneMatch");
			}
		} else {
			methodCall.setName("noneMatch");
		}

		return true;
	}

	private Optional<Expression> searchNegatingPredicate(Expression firstArgument) {
		if (!firstArgument.isLambdaExpr()) {
			return Optional.empty();
		}

		var lambda = firstArgument.asLambdaExpr();

		if (lambda.getExpressionBody().isEmpty()) {
			return Optional.empty();
		}

		Optional<Expression> optPredicate = lambda.getExpressionBody();
		if (optPredicate.isEmpty()) {
			return Optional.empty();
		}

		var predicate = optPredicate.get();
		if (predicate.isUnaryExpr() && predicate.asUnaryExpr().getOperator() == Operator.LOGICAL_COMPLEMENT) {
			return Optional.of(predicate.asUnaryExpr().getExpression());
		} else {
			return Optional.empty();
		}
	}

}
