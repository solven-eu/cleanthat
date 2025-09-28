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

/**
 * Switch o.toLowerCase().equals("some_string") to o.equalsIgnoreCase("some_string")
 *
 * @author Balazs Glatz
 */
public class UnnecessaryCaseChange extends AJavaparserExprMutator {

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
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof MethodCallExpr)) {
			return false;
		}

		var methodCall = (MethodCallExpr) node.getNode();
		var methodName = methodCall.getNameAsString();

		boolean equals = "equals".equals(methodName);
		boolean equalsIgnoreCase = METHOD_EQUALS_IGNORE_CASE.equals(methodName);

		if (!(equals || equalsIgnoreCase) || methodCall.getArguments().size() != 1) {
			return false;
		}

		Expression scope = methodCall.getScope().orElse(null);
		Expression argument = methodCall.getArgument(0);

		var left = new Side(scope);
		var right = new Side(argument);

		if (left.invalid || right.invalid) {
			return false;
		}

		if (left.isToLowerCase && right.isToLowerCase || left.isToUpperCase && right.isToUpperCase) {
			Expression newScope = getScope(left.methodCall);
			Expression newArg = getScope(right.methodCall);

			var replacement = new MethodCallExpr(newScope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(newArg));

			return tryReplace(methodCall, replacement);
		}

		if (left.isToLowerCase && right.isLowercase || left.isToUpperCase && right.isUppercase) {
			Expression newScope = getScope(left.methodCall);

			var replacement =
					new MethodCallExpr(newScope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(right.stringLiteral));

			return tryReplace(methodCall, replacement);
		}

		if (left.isLowercase && right.isToLowerCase || left.isUppercase && right.isToUpperCase) {
			Expression newArg = getScope(right.methodCall);

			var replacement =
					new MethodCallExpr(left.stringLiteral, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(newArg));

			return tryReplace(methodCall, replacement);
		}

		if (equalsIgnoreCase && left.isMethodCall && right.isMethodCall) {
			Expression newScope = getScope(left.methodCall);
			Expression newArg = getScope(right.methodCall);

			var replacement = new MethodCallExpr(newScope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(newArg));

			return tryReplace(methodCall, replacement);
		}

		if (equalsIgnoreCase && left.isMethodCall) {
			Expression newScope = getScope(left.methodCall);

			var replacement = new MethodCallExpr(newScope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(argument));

			return tryReplace(methodCall, replacement);
		}

		if (equalsIgnoreCase && right.isMethodCall) {
			Expression newArg = getScope(right.methodCall);

			var replacement = new MethodCallExpr(scope, METHOD_EQUALS_IGNORE_CASE, NodeList.nodeList(newArg));

			return tryReplace(methodCall, replacement);
		}

		return false;
	}

	private static Expression getScope(MethodCallExpr scope) {
		return scope.getScope().get().clone();
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
				methodCall = (MethodCallExpr) expression;

				if (!methodCall.getArguments().isEmpty() || !isString(methodCall)) {
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

		private static boolean isString(Expression expression) {
			String typeName = expression.calculateResolvedType().describe();
			return "java.lang.String".equals(typeName);
		}
	}

}
