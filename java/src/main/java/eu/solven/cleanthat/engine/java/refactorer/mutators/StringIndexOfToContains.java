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

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 's.indexOf(subString) >= 0' into 'c.contains(subString)' in String
 *
 * @author Benoit Lacelle
 */
public class StringIndexOfToContains extends AJavaparserExprMutator {

	final IntegerLiteralExpr zeroLiteral = new IntegerLiteralExpr("0");

	@Override
	public String minimalJavaVersion() {
		// String.contains has been introduced in JDK4
		return IJdkVersionConstants.JDK_4;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("IndexOfToContains");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/index-of-to-contains.html";
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	protected Class<?> expectedArgumentClass() {
		return String.class;
	}

	protected Class<?> expectedScopeClass() {
		return String.class;
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isBinaryExpr()) {
			return false;
		}
		var binaryExpr = expr.getNode().asBinaryExpr();

		boolean negateContains;

		Expression right = binaryExpr.getRight();
		Expression left = binaryExpr.getLeft();
		BinaryExpr.Operator operator = binaryExpr.getOperator();
		if (operator == BinaryExpr.Operator.GREATER_EQUALS && right.equals(zeroLiteral)
				|| left.equals(zeroLiteral) && operator == BinaryExpr.Operator.LESS_EQUALS) {
			// `s.indexOf(subString) >= 0`
			// ` 0 <= s.indexOf(subString)`
			negateContains = false;
		} else if (operator == BinaryExpr.Operator.LESS && right.equals(zeroLiteral)
				|| left.equals(zeroLiteral) && operator == BinaryExpr.Operator.GREATER) {
			// `s.indexOf(subString) < 0`
			// ` 0 > s.indexOf(subString)`
			negateContains = true;
		} else {
			return false;
		}

		Expression containsCall;
		if (isIndexOf(expr.editNode(left))) {
			MethodCallExpr indexOfCall = left.asMethodCallExpr();
			containsCall = new MethodCallExpr(indexOfCall.getScope().get(), "contains", indexOfCall.getArguments());
		} else if (isIndexOf(expr.editNode(right))) {
			MethodCallExpr indexOfCall = right.asMethodCallExpr();
			containsCall = new MethodCallExpr(indexOfCall.getScope().get(), "contains", indexOfCall.getArguments());
		} else {
			return false;
		}

		if (negateContains) {
			containsCall = new UnaryExpr(containsCall, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
		}

		return tryReplace(expr, containsCall);
	}

	private boolean isIndexOf(NodeAndSymbolSolver<Expression> left) {
		if (!left.getNode().isMethodCallExpr()) {
			return false;
		}
		MethodCallExpr methodCallExpr = left.getNode().asMethodCallExpr();
		if (!"indexOf".equals(methodCallExpr.getNameAsString())) {
			return false;
		} else if (methodCallExpr.getArguments().size() != 1) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(left.editNode(methodCallExpr.getScope()),
				expectedScopeClass())) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(left.editNode(methodCallExpr.getArgument(0)),
				expectedArgumentClass())) {
			return false;
		}

		return true;
	}
}
