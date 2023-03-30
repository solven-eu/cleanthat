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
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `list.isEmpty() ? Optional.empty() : Optional.of(list.get(0))` into `list.stream().findFirst()`
 *
 * @author Benoit Lacelle
 */
public class CollectionToOptional extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_2;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Collection");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isConditionalExpr()) {
			return false;
		}

		var conditionalExpr = expr.asConditionalExpr();

		var conditionExpr = conditionalExpr.getCondition();
		var thenExpr = conditionalExpr.getThenExpr();
		var elseExpr = conditionalExpr.getElseExpr();

		if (!conditionExpr.isMethodCallExpr() || !thenExpr.isMethodCallExpr() || !elseExpr.isMethodCallExpr()) {
			return false;
		}

		var conditionMethod = conditionExpr.asMethodCallExpr();
		var thenMethod = thenExpr.asMethodCallExpr();
		var elseMethod = elseExpr.asMethodCallExpr();

		if (!"isEmpty".equals(conditionMethod.getNameAsString())
				|| !scopeHasRequiredType(conditionMethod.getScope(), List.class)) {
			return false;
		} else if (!"Optional.empty()".equals(thenMethod.toString()) || !hasImported(expr, "java.util.Optional")) {
			return false;
		} else if (!"Optional.of(list.get(0))".equals(elseMethod.toString())) {
			// Given list is a List and Optional is imported, `Optional.of(list.get(0))` is not ambiguous
			return false;
		}

		return tryReplace(expr,
				new MethodCallExpr(new MethodCallExpr(conditionMethod.getScope().get(), "stream"), "findFirst"));
	}

	private boolean hasImported(Expression expr, String imported) {
		Optional<CompilationUnit> optCompilationUnit = expr.findCompilationUnit();
		if (optCompilationUnit.isEmpty()) {
			return false;
		}

		return optCompilationUnit.get()
				.getImports()
				.stream()
				.anyMatch(importDecl -> !importDecl.isAsterisk() && !importDecl.isStatic()
						&& imported.equals(importDecl.getNameAsString()));
	}
}
