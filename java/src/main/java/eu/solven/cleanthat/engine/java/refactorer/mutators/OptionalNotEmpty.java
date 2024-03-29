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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns '!o.isEmpty()' into 'o.isPresent()'
 *
 * @author Benoit Lacelle
 */
public class OptionalNotEmpty extends AJavaparserExprMutator {
	private static final String ID_NOTEMPTY = "OptionalNotEmpty";
	private static final String ID_ISPRESENT = "OptionalIsPresent";

	private static final String METHOD_IS_PRESENT = "isPresent";
	private static final String METHOD_IS_EMPTY = "isEmpty";

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Redundancy", "Optional");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Set<String> getIds() {
		return ImmutableSet.of(ID_NOTEMPTY, ID_ISPRESENT);
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.getNode().asMethodCallExpr();
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!METHOD_IS_EMPTY.equals(methodCallIdentifier) && !METHOD_IS_PRESENT.equals(methodCallIdentifier)) {
			return false;
		}
		Optional<Node> optParent = methodCall.getParentNode();
		// We looks for a negated expression '!optional.isEmpty()'
		if (methodCall.getScope().isEmpty() || optParent.isEmpty() || !(optParent.get() instanceof UnaryExpr)) {
			return false;
		}
		var unaryExpr = (UnaryExpr) optParent.get();
		if (!"LOGICAL_COMPLEMENT".equals(unaryExpr.getOperator().name())) {
			return false;
		}
		Optional<Expression> optScope = methodCall.getScope();

		if (!MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(optScope), Optional.class)) {
			return false;
		}

		var scope = optScope.get();
		String newMethod;

		if (METHOD_IS_EMPTY.equals(methodCallIdentifier)) {
			newMethod = METHOD_IS_PRESENT;
		} else {
			newMethod = METHOD_IS_EMPTY;
		}

		var localTransformed = false;
		var replacement = new MethodCallExpr(scope, newMethod);
		if (tryReplace(unaryExpr, replacement)) {
			localTransformed = true;
		}
		// TODO Add a rule to replace such trivial 'if else return'
		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}
}
