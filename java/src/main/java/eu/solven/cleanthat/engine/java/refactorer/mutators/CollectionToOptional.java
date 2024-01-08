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

import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ImportDeclarationHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns `list.isEmpty() ? Optional.empty() : Optional.of(list.get(0))` into `list.stream().findFirst()`
 *
 * @author Benoit Lacelle
 */
public class CollectionToOptional extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		// Optional and Stream has been introduced with JDK8
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Collection", "Optional");
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isConditionalExpr()) {
			return false;
		}

		var conditionalExpr = expr.getNode().asConditionalExpr();

		var ifExpr = conditionalExpr.getCondition();
		var thenExpr = conditionalExpr.getThenExpr();
		var elseExpr = conditionalExpr.getElseExpr();

		if (!ifExpr.isMethodCallExpr() || !thenExpr.isMethodCallExpr() || !elseExpr.isMethodCallExpr()) {
			return false;
		}

		var ifMethod = ifExpr.asMethodCallExpr();
		var thenMethod = thenExpr.asMethodCallExpr();
		var elseMethod = elseExpr.asMethodCallExpr();

		if (!"isEmpty".equals(ifMethod.getNameAsString())
				|| !MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(ifMethod.getScope()), List.class)) {
			return false;
		} else if (!"Optional.empty()".equals(thenMethod.toString()) || !hasImported(expr, "java.util.Optional")) {
			return false;
		} else if (!"Optional.of(list.get(0))".equals(elseMethod.toString())) {
			// Given list is a List and Optional is imported, `Optional.of(list.get(0))` is not ambiguous
			return false;
		}

		return tryReplace(expr.getNode(),
				new MethodCallExpr(new MethodCallExpr(ifMethod.getScope().get(), "stream"), "findFirst"));
	}

	private boolean hasImported(NodeAndSymbolSolver<Expression> expr, String imported) {
		return ImportDeclarationHelpers.isImported(expr, imported);
	}
}
