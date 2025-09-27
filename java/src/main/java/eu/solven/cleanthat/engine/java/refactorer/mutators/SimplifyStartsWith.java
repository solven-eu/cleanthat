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
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns `line.startsWith("#") || line.isEmpty()` into `line.isEmpty() || line.charAt('0') == '#'`
 *
 * @author Benoit Lacelle
 */
@Deprecated(since = "Dropped with PMD 7.0")
public class SimplifyStartsWith extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		// String.isEmpty has been introduced with JDK6
		return IJdkVersionConstants.JDK_6;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("SimplifyStartsWith");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd-doc-6.55.0/pmd_rules_java_performance.html#simplifystartswith";
	}

	@Override
	public Set<String> getLegacyIds() {
		return Set.of("StringStartsWithChar");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isBinaryExpr()) {
			return false;
		}
		var binaryExpr = expr.getNode().asBinaryExpr();

		boolean isAndNotEmptyElseOrEmpty;
		BinaryExpr.Operator operator = binaryExpr.getOperator();
		if (operator == BinaryExpr.Operator.AND) {
			// Expect to be in `line.startsWith("#") && !line.isEmpty()`
			isAndNotEmptyElseOrEmpty = true;
		} else if (operator == BinaryExpr.Operator.OR) {
			// Expect to be in `line.startsWith("#") || line.isEmpty()`
			isAndNotEmptyElseOrEmpty = false;
		} else {
			return false;
		}

		Optional<MethodCallExpr> optStartsWith = findStartsWith(expr, binaryExpr.getLeft(), binaryExpr.getRight());
		Optional<MethodCallExpr> optIsEmpty =
				findIsEmpty(expr, binaryExpr.getLeft(), binaryExpr.getRight(), isAndNotEmptyElseOrEmpty);

		if (optStartsWith.isEmpty() || optIsEmpty.isEmpty()) {
			return false;
		}

		var startsWithMethodCall = optStartsWith.get();
		var isEmptyMethodCall = optIsEmpty.get();
		// May be `line.isEmpty()` or `line.isEmpty()`
		Expression callIsEmptyExpr;
		if (isAndNotEmptyElseOrEmpty) {
			// This is supposed to be the UnaryExpr, parent of the MethodCallExpr
			callIsEmptyExpr = (Expression) isEmptyMethodCall.getParentNode().orElseThrow();
		} else {
			callIsEmptyExpr = isEmptyMethodCall;
		}

		Optional<Expression> optRightScope = isEmptyMethodCall.getScope();
		Optional<Expression> optLeftScope = startsWithMethodCall.getScope();

		if (optRightScope.isEmpty() || optLeftScope.isEmpty()) {
			return false;
		} else if (!sameScope(optLeftScope.get(), optRightScope.get())) {
			return false;
		}

		// Turns `line.startsWith("#") || line.isEmpty()` into `line.isEmpty() || line.startsWith("#")`
		binaryExpr.setLeft(callIsEmptyExpr);
		binaryExpr.setRight(startsWithMethodCall);

		var singleChar = optCallStartsWithSingleCharString(expr.editNode(startsWithMethodCall)).get();

		// Turns `line.startsWith("#")` into `line.indexOf('0') == '#'`
		return tryReplace(startsWithMethodCall, makeCharAtEqualsTo(optLeftScope, singleChar));

	}

	private Optional<MethodCallExpr> findIsEmpty(NodeAndSymbolSolver<?> context,
			Expression left,
			Expression right,
			boolean isAndNotEmptyElseOrEmpty) {
		return Stream.of(left, right)
				.flatMap(expr -> findIsEmpty(context, expr, isAndNotEmptyElseOrEmpty).stream())
				.findFirst();
	}

	private Optional<MethodCallExpr> findIsEmpty(NodeAndSymbolSolver<?> context,
			Expression left,
			boolean isAndNotEmptyElseOrEmpty) {
		if (isAndNotEmptyElseOrEmpty) {
			if (!left.isUnaryExpr()) {
				return Optional.empty();
			}
			if (left.asUnaryExpr().getOperator() != UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
				return Optional.empty();
			}
			left = left.asUnaryExpr().getExpression();
		}
		if (left.isMethodCallExpr() && isCallIsEmptyString(context.editNode(left.asMethodCallExpr()))) {
			return Optional.of(left.asMethodCallExpr());
		}

		return Optional.empty();
	}

	private Optional<MethodCallExpr> findStartsWith(NodeAndSymbolSolver<?> context, Expression left, Expression right) {
		if (left.isMethodCallExpr()
				&& optCallStartsWithSingleCharString(context.editNode(left.asMethodCallExpr())).isPresent()) {
			return Optional.of(left.asMethodCallExpr());
		} else if (right.isMethodCallExpr()
				&& optCallStartsWithSingleCharString(context.editNode(right.asMethodCallExpr())).isPresent()) {
			return Optional.of(right.asMethodCallExpr());
		} else {
			return Optional.empty();
		}
	}

	private BinaryExpr makeCharAtEqualsTo(Optional<Expression> optLeftScope, Character singleChar) {
		return new BinaryExpr(
				new MethodCallExpr(optLeftScope.get(), "charAt", new NodeList<>(new IntegerLiteralExpr("0"))),
				new CharLiteralExpr(singleChar),
				Operator.EQUALS);
	}

	private boolean sameScope(Expression left, Expression right) {
		return left.isNameExpr() && right.isNameExpr()
				&& left.asNameExpr().getNameAsString().equals(right.asNameExpr().getNameAsString());
	}

	private boolean isCallIsEmptyString(NodeAndSymbolSolver<MethodCallExpr> left) {
		return "isEmpty".equals(left.getNode().getNameAsString())
				&& MethodCallExprHelpers.scopeHasRequiredType(left.editNode(left.getNode().getScope()), String.class);
	}

	private Optional<Character> optCallStartsWithSingleCharString(NodeAndSymbolSolver<MethodCallExpr> expr) {
		MethodCallExpr methodCall = expr.getNode();
		if (!"startsWith".equals(expr.getNode().getNameAsString())
				|| !MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(methodCall.getScope()), String.class)) {
			return Optional.empty();
		}

		NodeList<Expression> arguments = methodCall.getArguments();
		if (arguments.size() != 1) {
			return Optional.empty();
		}

		var singleArgument = methodCall.getArgument(0);
		var hasSingleCharString =
				singleArgument.isStringLiteralExpr() && singleArgument.asStringLiteralExpr().getValue().length() == 1;
		if (hasSingleCharString) {
			return Optional.of(singleArgument.asStringLiteralExpr().getValue().charAt(0));
		}
		return Optional.empty();
	}
}
