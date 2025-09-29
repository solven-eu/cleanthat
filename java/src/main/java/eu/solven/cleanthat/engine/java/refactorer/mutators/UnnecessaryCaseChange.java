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

		if (isNotSingleArgumentEqualsOrEqualsIgnoreCase(equals, equalsIgnoreCase, methodCall)) {
			return false;
		}

		Expression originalScope = methodCall.getScope().orElse(null);
		if (!isCalledOnString(expression, originalScope)) {
			return false;
		}

		Expression originalArgument = methodCall.getArgument(0);

		UnnecessaryCaseChangeExpression scope;
		UnnecessaryCaseChangeExpression argument;
		try {
			scope = new UnnecessaryCaseChangeExpression(originalScope);
			argument = new UnnecessaryCaseChangeExpression(originalArgument);
		} catch (IllegalStateException exception) {
			return false;
		}

		// left.toLowerCase().equals(right.toLowerCase()) || left.toUpperCase().equals(right.toUpperCase())
		if (scope.isToLowerCase && argument.isToLowerCase || scope.isToUpperCase && argument.isToUpperCase) {
			Expression newScope = getScope(scope.methodCall);
			Expression newArg = getScope(argument.methodCall);

			var replacement = getReplacement(newScope, newArg);

			return tryReplace(methodCall, replacement);
		}

		// left.toLowerCase().equals("lowercase") || left.toUpperCase().equals("UPPERCASE")
		if (scope.isToLowerCase && argument.isLowercase || scope.isToUpperCase && argument.isUppercase) {
			Expression newScope = getScope(scope.methodCall);

			var replacement = getReplacement(newScope, argument.stringLiteral);

			return tryReplace(methodCall, replacement);
		}

		// "lowercase".equals(right.toLowerCase()) || "UPPERCASE".equals(right.toUpperCase())
		if (scope.isLowercase && argument.isToLowerCase || scope.isUppercase && argument.isToUpperCase) {
			Expression newArg = getScope(argument.methodCall);

			var replacement = getReplacement(scope.stringLiteral, newArg);

			return tryReplace(methodCall, replacement);
		}

		// left.toLowerCase().equalsIgnoreCase(right.toLowerCase())
		if (equalsIgnoreCase && scope.isMethodCall && argument.isMethodCall) {
			Expression newScope = getScope(scope.methodCall);
			Expression newArg = getScope(argument.methodCall);

			var replacement = getReplacement(newScope, newArg);

			return tryReplace(methodCall, replacement);
		}

		// left.toLowerCase().equalsIgnoreCase(right)
		if (equalsIgnoreCase && scope.isMethodCall) {
			Expression newScope = getScope(scope.methodCall);

			var replacement = getReplacement(newScope, originalArgument);

			return tryReplace(methodCall, replacement);
		}

		// left.equalsIgnoreCase(right.toLowerCase())
		if (equalsIgnoreCase && argument.isMethodCall) {
			Expression newArg = getScope(argument.methodCall);

			var replacement = getReplacement(originalScope, newArg);

			return tryReplace(methodCall, replacement);
		}

		return false;
	}

	private static boolean isNotSingleArgumentEqualsOrEqualsIgnoreCase(boolean equals,
			boolean equalsIgnoreCase,
			MethodCallExpr methodCall) {
		return !(equals || equalsIgnoreCase) || methodCall.getArguments().size() != 1;
	}

	private static boolean isCalledOnString(NodeAndSymbolSolver<Expression> expression, Expression originalScope) {
		return originalScope != null
				&& MethodCallExprHelpers.scopeHasRequiredType(expression.editNode(originalScope), String.class);
	}

	private static Expression getScope(MethodCallExpr scope) {
		return scope.getScope().get().clone();
	}

	private static MethodCallExpr getReplacement(Expression scope, Expression argument) {
		return new MethodCallExpr(scope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(argument));
	}

	@SuppressWarnings("PMD.ImmutableField")
	private static final class UnnecessaryCaseChangeExpression {
		private boolean isMethodCall;
		private MethodCallExpr methodCall;
		private StringLiteralExpr stringLiteral;

		private boolean isToLowerCase;
		private boolean isToUpperCase;

		private boolean isLowercase;
		private boolean isUppercase;

		/**
		 * @param expression
		 *            either the scope or the argument of an equals/equalsIgnoreCase method call
		 *
		 * @throws IllegalStateException
		 *             when the expression is neither a String literal nor a toLowerCase/toUpperCase method call
		 */
		@SuppressWarnings({ "PMD.UnnecessaryCaseChange", "PMD.UseLocaleWithCaseConversions" })
		private UnnecessaryCaseChangeExpression(Expression expression) {
			if (expression instanceof MethodCallExpr) {
				isMethodCall = true;
				methodCall = expression.asMethodCallExpr();

				if (!methodCall.getArguments().isEmpty()) {
					throw new IllegalStateException();
				}

				var methodName = methodCall.getNameAsString();
				isToLowerCase = "toLowerCase".equals(methodName);
				isToUpperCase = "toUpperCase".equals(methodName);

				if (!isToLowerCase && !isToUpperCase) {
					throw new IllegalStateException();
				}
			} else if (expression.isStringLiteralExpr()) {
				stringLiteral = expression.asStringLiteralExpr();
				String literal = stringLiteral.getValue();
				isLowercase = literal.equals(literal.toLowerCase());
				isUppercase = literal.equals(literal.toUpperCase());
			}
		}
	}

}
