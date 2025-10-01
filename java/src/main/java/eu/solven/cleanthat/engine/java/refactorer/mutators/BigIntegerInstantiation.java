/*
 * Copyright 2025 Benoit Lacelle - SOLVEN
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutatorDescriber;

/**
 * Turns `new BigInteger("1")` into `BigInteger.ONE`
 *
 * @author Balazs Glatz
 */
public class BigIntegerInstantiation extends AJavaparserExprMutator implements IMutatorDescriber {

	private static final Map<String, String> NUMBER_TO_CONSTANT = Map.of("0", "ZERO", "1", "ONE", "10", "TEN");

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
	}

	@Override
	public boolean isPerformanceImprovment() {
		return true;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Performance");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("BigIntegerInstantiation");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_performance.html#bigintegerinstantiation";
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expression) {
		var node = expression.getNode();

		var isObjectCreation = node instanceof ObjectCreationExpr;
		var isMethodCall = node instanceof MethodCallExpr;

		if (!(isObjectCreation || isMethodCall)) {
			return false;
		}

		var arguments = ((NodeWithArguments<?>) node).getArguments();

		if (arguments.size() != 1) {
			return false;
		}

		String typeName;
		if (isObjectCreation) {
			var objectCreation = node.asObjectCreationExpr();
			typeName = objectCreation.getType().getNameAsString();
		} else {
			var methodCall = node.asMethodCallExpr();
			if (!"valueOf".equals(methodCall.getNameAsString())) {
				return false;
			}
			Optional<Expression> scope = methodCall.getScope();
			var isBigInteger = MethodCallExprHelpers.scopeHasRequiredType(expression.editNode(scope), BigInteger.class);
			var isBigDecimal = MethodCallExprHelpers.scopeHasRequiredType(expression.editNode(scope), BigDecimal.class);
			if (isBigInteger) {
				typeName = "BigInteger";
			} else if (isBigDecimal) {
				typeName = "BigDecimal";
			} else {
				return false;
			}
		}

		var argument = arguments.get(0);

		String number;
		if (argument.isStringLiteralExpr()) {
			number = getValueAsString(typeName, argument.asStringLiteralExpr().getValue());
		} else if (argument.isDoubleLiteralExpr()) {
			number = getValueAsString(argument.asDoubleLiteralExpr().asDouble());
		} else {
			number = getValueAsString(typeName, getArgumentAsNumber(argument));
		}

		if (number == null) {
			return false;
		}
		var constant = NUMBER_TO_CONSTANT.get(number);
		if (constant == null) {
			return false;
		}

		var replacement = new FieldAccessExpr(new NameExpr(typeName), constant);

		return tryReplace(expression, replacement);
	}

	private Long getArgumentAsNumber(Expression argument) {
		if (argument.isIntegerLiteralExpr()) {
			return argument.asIntegerLiteralExpr().asNumber().longValue();
		} else if (argument.isLongLiteralExpr()) {
			return argument.asLongLiteralExpr().asNumber().longValue();
		} else {
			return null;
		}
	}

	public static String getValueAsString(String klass, String value) {
		try {
			if ("BigDecimal".equals(klass)) {
				return new BigDecimal(value).toString();
			}
			return new BigInteger(value).toString();
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	public static String getValueAsString(double value) {
		try {
			return BigDecimal.valueOf(value).toString();
		} catch (ClassCastException | NumberFormatException ignored) {
			return null;
		}
	}

	public static String getValueAsString(String klass, Long value) {
		if (value == null) {
			return null;
		}
		try {
			if ("BigDecimal".equals(klass)) {
				return BigDecimal.valueOf(value).toString();
			}
			return BigInteger.valueOf(value).toString();
		} catch (ClassCastException | NumberFormatException exception) {
			return null;
		}
	}

}
