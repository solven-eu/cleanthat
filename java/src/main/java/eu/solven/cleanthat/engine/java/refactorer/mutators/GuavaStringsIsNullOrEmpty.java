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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns 's == null || s.isEmpty()` into `Strings.isNullOrEmpty(s)`
 *
 * @author Benoit Lacelle
 */
// https://errorprone.info/docs/inlineme
// see com.google.common.base.Strings.repeat(String, int)
public class GuavaStringsIsNullOrEmpty extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Guava", "String");
	}

	public String minimalGuavaVersion() {
		return "3.0";
	}

	@Override
	public Optional<String> getCleanthatId() {
		return Optional.of("GuavaStringsIsNullOrEmpty");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isBinaryExpr()) {
			return false;
		}
		var binaryExpr = expr.asBinaryExpr();

		if (binaryExpr.getOperator() != Operator.OR) {
			return false;
		}

		var left = binaryExpr.getLeft();
		var right = binaryExpr.getRight();

		Optional<Expression> stringIsNullOrEmpty = oneIsNullOtherIsEmpty(left, right);
		if (stringIsNullOrEmpty.isEmpty()) {
			return false;
		}

		Optional<CompilationUnit> optCompilationUnit = expr.findCompilationUnit();
		if (optCompilationUnit.isEmpty()) {
			return false;
		}
		optCompilationUnit.get().addImport(Strings.class.getName());

		var replacement =
				new MethodCallExpr(new NameExpr("Strings"), "isNullOrEmpty", new NodeList<>(stringIsNullOrEmpty.get()));

		return tryReplace(binaryExpr, replacement);
	}

	private Optional<Expression> oneIsNullOtherIsEmpty(Expression left, Expression right) {
		Optional<Expression> leftIsNullCheck = searchNullCheck(left);

		if (leftIsNullCheck.isPresent()) {
			if (isEmpty(right, leftIsNullCheck.get())) {
				return right.asMethodCallExpr().getScope();
			} else {
				return Optional.empty();
			}
		} else {
			Optional<Expression> rightIsNullCheck = searchNullCheck(right);
			if (rightIsNullCheck.isPresent() && isEmpty(left, rightIsNullCheck.get())) {
				return left.asMethodCallExpr().getScope();
			} else {
				return Optional.empty();
			}
		}
	}

	private boolean isEmpty(Expression shouldCallIsEmpty, Expression isNullChecked) {
		if (!shouldCallIsEmpty.isMethodCallExpr()) {
			return false;
		}
		var asMethodCallExpr = shouldCallIsEmpty.asMethodCallExpr();

		if (asMethodCallExpr.getScope().isEmpty()) {
			return false;
		}

		if (!"isEmpty".equals(asMethodCallExpr.getNameAsString())
				|| !scopeHasRequiredType(asMethodCallExpr.getScope(), String.class)) {
			return false;
		}

		return asMethodCallExpr.getScope().get().equals(isNullChecked);
	}

	private Optional<Expression> searchNullCheck(Expression expr) {
		if (!expr.isBinaryExpr()) {
			return Optional.empty();
		}
		var binaryExpr = expr.asBinaryExpr();

		if (binaryExpr.getLeft().isNullLiteralExpr()) {
			return Optional.of(binaryExpr.getRight());
		} else if (binaryExpr.getRight().isNullLiteralExpr()) {
			return Optional.of(binaryExpr.getLeft());
		} else {
			return Optional.empty();
		}
	}
}
