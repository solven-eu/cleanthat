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
import java.util.stream.Stream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `line.startsWith("#") || line.isEmpty()` into `line.isEmpty() || line.charAt('0') == '#'`
 *
 * @author Benoit Lacelle
 */
public class SimplifyStartsWith extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		// String.isEmpty has been introduced with JDK6
		return IJdkVersionConstants.JDK_6;
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("SimplifyStartsWith");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#simplifystartswith";
	}

	@Override
	public Optional<String> getCleanthatId() {
		return Optional.of("StringStartsWithChar");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isBinaryExpr()) {
			return false;
		}
		var binaryExpr = expr.asBinaryExpr();

		boolean isAndNotEmptyElseOrEmpty;
		if (binaryExpr.getOperator() == BinaryExpr.Operator.AND) {
			// Expect to be in `line.startsWith("#") && !line.isEmpty()`
			isAndNotEmptyElseOrEmpty = true;
		} else if (binaryExpr.getOperator() == BinaryExpr.Operator.OR) {
			// Expect to be in `line.startsWith("#") || line.isEmpty()`
			isAndNotEmptyElseOrEmpty = false;
		} else {
			return false;
		}

		Optional<MethodCallExpr> optStartsWith = findStartsWith(binaryExpr.getLeft(), binaryExpr.getRight());
		Optional<MethodCallExpr> optIsEmpty =
				findIsEmpty(binaryExpr.getLeft(), binaryExpr.getRight(), isAndNotEmptyElseOrEmpty);

		if (optStartsWith.isEmpty() || optIsEmpty.isEmpty()) {
			return false;
		}

		var startsWithMethodCall = optStartsWith.get();
		var isEmptyMethodCall = optIsEmpty.get();
		// May be `line.isEmpty()` or `line.isEmpty()`
		Expression isEmptyExpr;
		if (isAndNotEmptyElseOrEmpty) {
			// This is supposed to be the UnaryExpr, parent of the MethodCallExpr
			isEmptyExpr = (Expression) isEmptyMethodCall.getParentNode().orElseThrow();
		} else {
			isEmptyExpr = isEmptyMethodCall;
		}

		Optional<Expression> optRightScope = isEmptyMethodCall.getScope();
		Optional<Expression> optLeftScope = startsWithMethodCall.getScope();

		if (optRightScope.isEmpty() || optLeftScope.isEmpty()) {
			return false;
		} else if (!sameScope(optLeftScope.get(), optRightScope.get())) {
			return false;
		}

		// Turns `line.startsWith("#") || line.isEmpty()` into `line.isEmpty() || line.startsWith("#")`
		binaryExpr.setLeft(isEmptyExpr);
		binaryExpr.setRight(startsWithMethodCall);

		var singleChar = optCallStartsWithSingleCharString(startsWithMethodCall).get();

		// Turns `line.startsWith("#")` into `line.indexOf('0') == '#'`
		return replaceBy(startsWithMethodCall, makeCharAtEqualsTo(optLeftScope, singleChar));

	}

	private Optional<MethodCallExpr> findIsEmpty(Expression left, Expression right, boolean isAndNotEmptyElseOrEmpty) {
		return Stream.of(left, right).flatMap(expr -> findIsEmpty(expr, isAndNotEmptyElseOrEmpty).stream()).findFirst();
	}

	private Optional<MethodCallExpr> findIsEmpty(Expression left, boolean isAndNotEmptyElseOrEmpty) {
		if (isAndNotEmptyElseOrEmpty) {
			if (!left.isUnaryExpr()) {
				return Optional.empty();
			}
			if (left.asUnaryExpr().getOperator() != UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
				return Optional.empty();
			}
			left = left.asUnaryExpr().getExpression();
		}
		if (left.isMethodCallExpr() && isCallIsEmptyString(left.asMethodCallExpr())) {
			return Optional.of(left.asMethodCallExpr());
		}

		return Optional.empty();
	}

	private Optional<MethodCallExpr> findStartsWith(Expression left, Expression right) {
		if (left.isMethodCallExpr() && optCallStartsWithSingleCharString(left.asMethodCallExpr()).isPresent()) {
			return Optional.of(left.asMethodCallExpr());
		} else if (right.isMethodCallExpr()
				&& optCallStartsWithSingleCharString(right.asMethodCallExpr()).isPresent()) {
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

	private boolean isCallIsEmptyString(MethodCallExpr left) {
		return "isEmpty".equals(left.asMethodCallExpr().getNameAsString())
				&& scopeHasRequiredType(left.asMethodCallExpr().getScope(), String.class);
	}

	private Optional<Character> optCallStartsWithSingleCharString(MethodCallExpr expr) {
		if (!"startsWith".equals(expr.getNameAsString()) || !scopeHasRequiredType(expr.getScope(), String.class)) {
			return Optional.empty();
		}

		NodeList<Expression> arguments = expr.getArguments();
		if (arguments.size() != 1) {
			return Optional.empty();
		}

		var singleArgument = expr.getArgument(0);
		var hasSingleCharString =
				singleArgument.isStringLiteralExpr() && singleArgument.asStringLiteralExpr().getValue().length() == 1;
		if (hasSingleCharString) {
			return Optional.of(singleArgument.asStringLiteralExpr().getValue().charAt(0));
		}
		return Optional.empty();
	}
}
