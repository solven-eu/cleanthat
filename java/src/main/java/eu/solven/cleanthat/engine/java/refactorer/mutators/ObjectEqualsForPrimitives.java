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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 'Object.equals(1, 5)` into `1 == 5`
 *
 * @author Benoit Lacelle
 */
// https://errorprone.info/bugpattern/ObjectEqualsForPrimitives
public class ObjectEqualsForPrimitives extends AJavaparserExprMutator {
	private static final List<Class<?>> PRIMITIVE_CLASSES =
			Arrays.asList(int.class, long.class, float.class, double.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	public Optional<String> getErrorProneId() {
		return Optional.of("ObjectEqualsForPrimitives");
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.getNode().asMethodCallExpr();
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!"equals".equals(methodCallIdentifier)) {
			return false;
		}

		if (methodCall.getArguments().size() != 2) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();

		if (optScope.isEmpty() || !optScope.get().isNameExpr()
				|| !"Objects".equals(optScope.get().asNameExpr().getNameAsString())) {
			return false;
		}

		var left = methodCall.getArgument(0);
		var right = methodCall.getArgument(1);

		if (PRIMITIVE_CLASSES.stream()
				.anyMatch(c -> MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(left), c)
						&& MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(right), c))) {
			var replacement = new BinaryExpr(left, right, Operator.EQUALS);
			return tryReplace(expr, replacement);
		} else {
			return false;
		}
	}
}
