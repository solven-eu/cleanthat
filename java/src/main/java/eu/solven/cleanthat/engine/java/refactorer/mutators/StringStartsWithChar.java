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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `line.startsWith("#") || line.isEmpty()` into `line.isEmpty() || line.charAt('0') == '#'`
 *
 * @author Benoit Lacelle
 */
public class StringStartsWithChar extends AJavaparserExprMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(StringStartsWithChar.class);

	@Override
	public String minimalJavaVersion() {
		// String.isEmpty has been introduced with JDK6
		return IJdkVersionConstants.JDK_6;
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

		if (binaryExpr.getOperator() != Operator.OR) {
			return false;
		}

		Expression left = binaryExpr.getLeft();
		if (!left.isMethodCallExpr()) {
			return false;
		}

		Expression right = binaryExpr.getRight();
		if (!right.isMethodCallExpr()) {
			return false;
		}

		MethodCallExpr leftMethodCall = left.asMethodCallExpr();
		MethodCallExpr rightMethodCall = right.asMethodCallExpr();
		Optional<Expression> optRightScope = rightMethodCall.getScope();
		Optional<Expression> optLeftScope = leftMethodCall.getScope();

		if (optRightScope.isEmpty() || optLeftScope.isEmpty()) {
			return false;
		} else if (!sameScope(optLeftScope.get(), optRightScope.get())) {
			return false;
		}

		if (isCallIsEmptyString(leftMethodCall) && isCallStartsWithSingleCharString(rightMethodCall).isPresent()) {
			Character singleChar = isCallStartsWithSingleCharString(rightMethodCall).get();
			return replaceBy(right, makeCharAtEqualsTo(optRightScope, singleChar));
		} else if (isCallStartsWithSingleCharString(leftMethodCall).isPresent()
				&& isCallIsEmptyString(rightMethodCall)) {
			// Turns `line.startsWith("#") || line.isEmpty()` into `line.isEmpty() || line.startsWith("#")`
			binaryExpr.setLeft(right);
			binaryExpr.setRight(left);

			Character singleChar = isCallStartsWithSingleCharString(leftMethodCall).get();

			// Turns `line.startsWith("#")` into `line.indexOf('0') == '#'`
			return replaceBy(left, makeCharAtEqualsTo(optLeftScope, singleChar));
		} else {
			return false;
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
		return left.asMethodCallExpr().getNameAsString().equals("isEmpty")
				&& scopeHasRequiredType(left.asMethodCallExpr().getScope(), String.class);
	}

	private Optional<Character> isCallStartsWithSingleCharString(MethodCallExpr left) {
		boolean hasSingleCharString =
				left.getNameAsString().equals("startsWith") && scopeHasRequiredType(left.getScope(), String.class)
						&& left.getArguments().size() == 1
						&& left.getArgument(0).isStringLiteralExpr()
						&& left.getArgument(0).asStringLiteralExpr().getValue().length() == 1;
		if (hasSingleCharString) {
			return Optional.of(left.getArgument(0).asStringLiteralExpr().getValue().charAt(0));
		}
		return Optional.empty();
	}
}
