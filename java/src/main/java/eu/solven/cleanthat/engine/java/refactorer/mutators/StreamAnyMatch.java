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
import java.util.stream.Stream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 's.filter(p).findAny().isPresent()' into 's.anyMatch(predicate)'
 *
 * @author Benoit Lacelle
 */
public class StreamAnyMatch extends AJavaparserNodeMutator {
	private static final String METHOD_FILTER = "filter";
	private static final String METHOD_FIND_ANY = "findAny";
	private static final String METHOD_IS_PRESENT = "isPresent";
	private static final String METHOD_IS_EMPTY = "isEmpty";

	private static final String METHOD_ANY_MATCH = "anyMatch";

	// Stream exists since 8
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-4034");
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof MethodCallExpr)) {
			return false;
		}
		var methodCall = (MethodCallExpr) node.getNode();
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!METHOD_IS_PRESENT.equals(methodCallIdentifier) && !METHOD_IS_EMPTY.equals(methodCallIdentifier)) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			return false;
		}
		var scope = optScope.get();

		if (!(scope instanceof MethodCallExpr)) {
			return false;
		}
		var scopeAsMethodCallExpr = (MethodCallExpr) scope;
		if (!METHOD_FIND_ANY.equals(scopeAsMethodCallExpr.getName().getIdentifier())) {
			return false;
		}

		Optional<Expression> optParentScope = scopeAsMethodCallExpr.getScope();
		if (optParentScope.isEmpty()) {
			return false;
		}
		var parentScope = optParentScope.get();
		if (!parentScope.isMethodCallExpr()) {
			return false;
		}
		var parentScopeAsMethodCallExpr = (MethodCallExpr) parentScope;
		if (!METHOD_FILTER.equals(parentScopeAsMethodCallExpr.getName().getIdentifier())) {
			return false;
		}

		Optional<Expression> optGrandParentScope = parentScopeAsMethodCallExpr.getScope();
		if (optGrandParentScope.isEmpty()) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(node.editNode(optGrandParentScope), Stream.class)) {
			return false;
		}
		var grandParentScope = optGrandParentScope.get();

		var filterPredicate = parentScopeAsMethodCallExpr.getArgument(0);

		var localTransformed = false;
		NodeList<Expression> replaceArguments = new NodeList<>(filterPredicate);
		Expression replacement = new MethodCallExpr(grandParentScope, METHOD_ANY_MATCH, replaceArguments);

		if (METHOD_IS_EMPTY.equals(methodCallIdentifier)) {
			replacement = new UnaryExpr(replacement, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
		}
		if (tryReplace(methodCall, replacement)) {
			localTransformed = true;
		}

		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}
}
