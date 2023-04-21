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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 'c.removeAll(c)' into 'c.clear()' in Collection
 *
 * @author Benoit Lacelle
 */
public class RemoveAllToClearCollection extends AJavaparserExprMutator {

	@Override
	public String minimalJavaVersion() {
		// Collection has been introduced in JDK2
		return IJdkVersionConstants.JDK_2;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Collection");
	}

	@Override
	public Optional<String> getSpotBugsId() {
		return Optional.of("DMI_USING_REMOVEALL_TO_CLEAR_COLLECTION");
	}

	@Override
	public String spotBugsUrl() {
		return "https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#dmi-using-removeall-to-clear-collection";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.getNode().asMethodCallExpr();

		if (!"removeAll".equals(methodCall.getNameAsString())) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(methodCall.getScope()),
				Collection.class)) {
			return false;
		} else if (methodCall.getArguments().size() != 1) {
			return false;
		} else if (isMethodReturnUsed(methodCall)) {
			return false;
		}

		Expression scope = methodCall.getScope().get();
		if (!scope.equals(methodCall.getArgument(0))) {
			return false;
		}

		return tryReplace(methodCall, new MethodCallExpr(scope, "clear"));
	}
}
