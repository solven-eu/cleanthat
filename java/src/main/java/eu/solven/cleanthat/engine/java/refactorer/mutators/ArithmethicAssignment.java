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

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.BinaryExprHelpers;

/**
 * Turns 'i = i + 3;` into `i = i + (3 + 4);`
 *
 * @author Benoit Lacelle
 */
public class ArithmethicAssignment extends AJavaparserExprMutator {

	private static final Set<BinaryExpr.Operator> MAY_TURN_ASSIGN_OPERATOR = Set.of(BinaryExpr.Operator.PLUS,
			BinaryExpr.Operator.MINUS,
			BinaryExpr.Operator.MULTIPLY,
			BinaryExpr.Operator.DIVIDE);

	private static final Set<BinaryExpr.Operator> SYMETRIC_OPERATORS =
			Set.of(BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MULTIPLY);

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("ArithmethicAssignment");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/arithmethic-assignment.html";
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2164");
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isAssignExpr()) {
			return false;
		}
		var assignExpr = expr.asAssignExpr();

		if (assignExpr.getOperator() != AssignExpr.Operator.ASSIGN) {
			// We can use turn from the assign operation `=`
			return false;
		}

		if (!assignExpr.getTarget().isNameExpr()) {
			return false;
		} else if (!assignExpr.getValue().isBinaryExpr()) {
			// TODO Cut through EnclosingExpr
			return false;
		}

		NameExpr target = assignExpr.getTarget().asNameExpr();

		BinaryExpr binaryExpr = assignExpr.getValue().asBinaryExpr();
		Optional<Entry<Expression, Expression>> optValueIfValue;

		if (SYMETRIC_OPERATORS.contains(binaryExpr.getOperator())) {
			optValueIfValue = BinaryExprHelpers.findPair(binaryExpr, n -> n.equals(target), n -> true);
		} else {
			if (binaryExpr.getLeft().equals(target)) {
				optValueIfValue = Optional.of(Maps.immutableEntry(binaryExpr.getLeft(), binaryExpr.getRight()));
			} else {
				optValueIfValue = Optional.empty();
			}
		}
		if (optValueIfValue.isEmpty()) {
			return false;
		} else if (!MAY_TURN_ASSIGN_OPERATOR.contains(binaryExpr.getOperator())) {
			return false;
		}

		boolean replaced = tryReplace(binaryExpr, optValueIfValue.get().getValue());

		if (replaced) {
			assignExpr.setOperator(toAssignOperator(binaryExpr.getOperator()));
		}

		return replaced;
	}

	private AssignExpr.Operator toAssignOperator(BinaryExpr.Operator operator) {
		switch (operator) {
		case PLUS:
			return AssignExpr.Operator.PLUS;
		case MINUS:
			return AssignExpr.Operator.MINUS;
		case MULTIPLY:
			return AssignExpr.Operator.MULTIPLY;
		case DIVIDE:
			return AssignExpr.Operator.DIVIDE;
		default:
			throw new IllegalArgumentException("Invalid operator: " + operator);
		}
	}
}
