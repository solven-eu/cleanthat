/*
 * Copyright 2025 Benoit Lacelle - SOLVEN
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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Switch o.toLowerCase().equals("some_string") to o.equalsIgnoreCase("some_string")
 *
 * @author Balazs Glatz
 */
public class UnnecessaryCaseChange extends AJavaparserExprMutator {

	private static final String METHOD_EQUALS = "equals";
	private static final String METHOD_EQUALS_IGNORE_CASE = "equalsIgnoreCase";

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UnnecessaryCaseChange");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#unnecessarycasechange";
	}

	@Override
	@SuppressWarnings("PMD.NPathComplexity")
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expression) {
		if (!(expression.getNode() instanceof MethodCallExpr)) {
			return false;
		}

		var methodCall = expression.getNode().asMethodCallExpr();

		var methodName = methodCall.getNameAsString();
		boolean equals = METHOD_EQUALS.equals(methodName);
		boolean equalsIgnoreCase = METHOD_EQUALS_IGNORE_CASE.equals(methodName);

		if (!(equals || equalsIgnoreCase) || methodCall.getArguments().size() != 1) {
			return false;
		}

		Optional<Expression> scope = methodCall.getScope();
		if (!MethodCallExprHelpers.scopeHasRequiredType(expression.editNode(scope), String.class)) {
			return false;
		}

		Expression argument = methodCall.getArgument(0);

		var left = new Side(scope.get());
		var right = new Side(argument);

		if (left.invalid || right.invalid) {
			return false;
		}

		if (left.isToLowerCase && right.isToLowerCase || left.isToUpperCase && right.isToUpperCase) {
			Expression newScope = getScope(left.methodCall);
			Expression newArg = getScope(right.methodCall);

			var replacement = getReplacement(newScope, newArg);

			return tryReplace(methodCall, replacement);
		}

		if (left.isToLowerCase && right.isLowercase || left.isToUpperCase && right.isUppercase) {
			Expression newScope = getScope(left.methodCall);

			var replacement = getReplacement(newScope, right.stringLiteral);

			return tryReplace(methodCall, replacement);
		}

		if (left.isLowercase && right.isToLowerCase || left.isUppercase && right.isToUpperCase) {
			Expression newArg = getScope(right.methodCall);

			var replacement = getReplacement(left.stringLiteral, newArg);

			return tryReplace(methodCall, replacement);
		}

		if (equalsIgnoreCase && left.isMethodCall && right.isMethodCall) {
			Expression newScope = getScope(left.methodCall);
			Expression newArg = getScope(right.methodCall);

			var replacement = getReplacement(newScope, newArg);

			return tryReplace(methodCall, replacement);
		}

		if (equalsIgnoreCase && left.isMethodCall) {
			Expression newScope = getScope(left.methodCall);

			var replacement = getReplacement(newScope, argument);

			return tryReplace(methodCall, replacement);
		}

		if (equalsIgnoreCase && right.isMethodCall) {
			Expression newArg = getScope(right.methodCall);

			var replacement = getReplacement(scope.get(), newArg);

			return tryReplace(methodCall, replacement);
		}

		return false;
	}

	private static Expression getScope(MethodCallExpr scope) {
		return scope.getScope().get().clone();
	}

	private static MethodCallExpr getReplacement(Expression scope, Expression argument) {
		return new MethodCallExpr(scope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(argument));
	}

	@SuppressWarnings("PMD.ImmutableField")
	private static final class Side {
		private boolean isMethodCall;
		private MethodCallExpr methodCall;
		private StringLiteralExpr stringLiteral;

		private boolean invalid;

		private boolean isToLowerCase;
		private boolean isToUpperCase;

		private boolean isLowercase;
		private boolean isUppercase;

		@SuppressWarnings({ "PMD.UnnecessaryCaseChange", "PMD.UseLocaleWithCaseConversions" })
		private Side(Expression expression) {
			if (expression instanceof MethodCallExpr) {
				isMethodCall = true;
				methodCall = expression.asMethodCallExpr();

				if (!methodCall.getArguments().isEmpty()) {
					invalid = true;
					return;
				}

				var methodName = methodCall.getNameAsString();
				isToLowerCase = "toLowerCase".equals(methodName);
				isToUpperCase = "toUpperCase".equals(methodName);

				if (!isToLowerCase && !isToUpperCase) {
					invalid = true;
					return;
				}
			}

			if (!expression.isStringLiteralExpr()) {
				return;
			}

			stringLiteral = expression.asStringLiteralExpr();
			String literal = stringLiteral.getValue();
			isLowercase = literal.equals(literal.toLowerCase());
			isUppercase = literal.equals(literal.toUpperCase());
		}
	}

}
