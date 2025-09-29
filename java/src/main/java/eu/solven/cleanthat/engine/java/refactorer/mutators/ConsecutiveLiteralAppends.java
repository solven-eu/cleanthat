/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Switch builder.append("string").append("builder") to builder.append("stringbuilder")
 *
 * @author Balazs Glatz
 */
public class ConsecutiveLiteralAppends extends AJavaparserExprMutator {

	private static final int LARGEST_SINGLE_DIGIT_NUMBER = 9;
	private static final int SMALLEST_SINGLE_DIGIT_NUMBER = 0;

	private static final String METHOD_APPEND = "append";

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("ConsecutiveLiteralAppends");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_performance.html#consecutiveliteralappends";
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expression) {
		if (!(expression.getNode() instanceof MethodCallExpr)) {
			return false;
		}

		var methodCall = expression.getNode().asMethodCallExpr();
		if (!isAppendWithSingleParam(methodCall)) {
			return false;
		}

		String argument = getStringValue(methodCall.getArgument(SMALLEST_SINGLE_DIGIT_NUMBER));
		if (argument == null) {
			return false;
		}

		Optional<Expression> scope = methodCall.getScope();
		if (scope.isEmpty()) {
			return false;
		}

		if (!(scope.get() instanceof MethodCallExpr) || !isAppendableScope(expression, scope)) {
			return false;
		}

		var previousMethodCall = scope.get().asMethodCallExpr();
		if (!isAppendWithSingleParam(previousMethodCall)) {
			return false;
		}

		if (!isAppendableScope(expression, previousMethodCall.getScope())) {
			return false;
		}

		String previousArgument = getStringValue(previousMethodCall.getArgument(SMALLEST_SINGLE_DIGIT_NUMBER));
		if (previousArgument == null) {
			return false;
		}

		var newArgument = new StringLiteralExpr(previousArgument + argument);
		var replacement =
				new MethodCallExpr(previousMethodCall.getScope().get(), METHOD_APPEND, NodeList.nodeList(newArgument));

		return tryReplace(expression, replacement);
	}

	private static boolean isAppendWithSingleParam(MethodCallExpr methodCall) {
		return METHOD_APPEND.equals(methodCall.getNameAsString()) && methodCall.getArguments().size() == 1;
	}

	private static boolean isAppendableScope(NodeAndSymbolSolver<Expression> expression, Optional<Expression> scope) {
		return MethodCallExprHelpers.scopeHasRequiredType(expression.editNode(scope), Appendable.class);
	}

	private static String getStringValue(Expression argument) {
		if (argument.isStringLiteralExpr()) {
			return argument.asStringLiteralExpr().getValue();
		}
		if (argument.isCharLiteralExpr()) {
			return argument.asCharLiteralExpr().getValue();
		}
		if (argument.isIntegerLiteralExpr()) {
			var intValue = argument.asIntegerLiteralExpr().asNumber().intValue();
			if (SMALLEST_SINGLE_DIGIT_NUMBER <= intValue && intValue <= LARGEST_SINGLE_DIGIT_NUMBER) {
				return String.valueOf(intValue);
			}
		}
		return null;
	}

}
