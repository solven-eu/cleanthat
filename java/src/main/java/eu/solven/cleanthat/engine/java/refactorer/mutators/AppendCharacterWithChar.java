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

import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.utils.StringEscapeUtils;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns `stringBuilder.append("c")` into `stringBuilder.append('c')`
 * <p>
 * Works with classes listed in {@code SUPPORTED_TYPES}.
 *
 * @author Balazs Glatz
 */
public class AppendCharacterWithChar extends AJavaparserExprMutator {

	public static final String METHOD_APPEND = "append";

	private static final Set<String> SUPPORTED_TYPES = Set
			.of("java.io.CharArrayWriter", "java.io.StringWriter", "java.lang.StringBuffer", "java.lang.StringBuilder");

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("AppendCharacterWithChar");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#appendcharacterwithchar";
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> node) {
		if (!(node.getNode() instanceof MethodCallExpr)) {
			return false;
		}

		var methodCall = node.getNode().asMethodCallExpr();
		var methodName = methodCall.getNameAsString();
		if (!METHOD_APPEND.equals(methodName) || methodCall.getArguments().size() != 1) {
			return false;
		}

		Expression argument = methodCall.getArgument(0);
		if (!argument.isStringLiteralExpr()) {
			return false;
		}

		Expression scope = methodCall.getScope().orElse(null);
		if (!isSupportedType(scope)) {
			return false;
		}

		var argumentAsString = argument.asStringLiteralExpr().getValue();
		if (couldBeCharacter(argumentAsString)) {
			return false;
		}

		CharLiteralExpr character = new CharLiteralExpr(argumentAsString);
		var replacement = new MethodCallExpr(scope, METHOD_APPEND, NodeList.nodeList(character));

		return tryReplace(methodCall, replacement);
	}

	private static boolean isSupportedType(Expression expression) {
		if (expression == null) {
			return false;
		}
		String typeName = expression.calculateResolvedType().describe();
		return SUPPORTED_TYPES.contains(typeName);
	}

	private static boolean couldBeCharacter(String argumentAsString) {
		return StringEscapeUtils.unescapeJava(argumentAsString).length() != 1;
	}

}
