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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutatorDescriber;

/**
 * Turns `new BigInteger("1")` into `BigInteger.ONE`
 *
 * @author Balazs Glatz
 */
public class BigIntegerInstantiation extends AJavaparserExprMutator implements IMutatorDescriber {

	private static final String NOTHING = "";

	private static final String NUMERIC_LITERAL_SUFFIX_REGEX = "(?<=\\d)(\\.0?)?([ldfLDF])?$";

	private static final Map<String, String> CONSTANTS = Map.of("0", "ZERO", "1", "ONE", "10", "TEN");

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
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

	/**
	 * {@link BigIntegerInstantiation} may turn NumberFormatException into a BigInteger.
	 */
	@Override
	public boolean isPreventingExceptions() {
		return true;
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expression) {
		if (!(expression.getNode() instanceof ObjectCreationExpr)) {
			return false;
		}

		var objectCreation = expression.getNode().asObjectCreationExpr();
		if (!isSingleArgumentObjectCreation(objectCreation)) {
			return false;
		}

		String typeName = objectCreation.getType().getNameAsString();
		Expression argument = objectCreation.getArgument(0);
		if (!isSupportedObjectCreation(typeName, argument)) {
			return false;
		}

		String value = getValue((LiteralStringValueExpr) argument);
		String constant = CONSTANTS.get(value);
		if (constant == null) {
			return false;
		}

		var replacement = new FieldAccessExpr(new NameExpr(typeName), constant);

		return tryReplace(expression, replacement);
	}

	private static boolean isSingleArgumentObjectCreation(ObjectCreationExpr objectCreation) {
		return objectCreation.getArguments().size() == 1;
	}

	private static boolean isSupportedObjectCreation(String typeName, Expression argument) {
		return ("BigDecimal".equals(typeName) || "BigInteger".equals(typeName)) && isSupportedArgumentType(argument);
	}

	private static boolean isSupportedArgumentType(Expression argument) {
		return argument.isStringLiteralExpr() || argument.isIntegerLiteralExpr()
				|| argument.isLongLiteralExpr()
				|| argument.isDoubleLiteralExpr();
	}

	private static String getValue(LiteralStringValueExpr argument) {
		return argument.getValue().replaceAll(NUMERIC_LITERAL_SUFFIX_REGEX, NOTHING);
	}

}
