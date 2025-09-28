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
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Switch o.toLowerCase().equals("some_string") to o.equalsIgnoreCase("some_string")
 *
 * @author Balazs Glatz
 */
public class UnnecessaryCaseChange extends AJavaparserExprMutator {

	private static final Map<String, Predicate<String>> CASE_CHECKERS = Map
			.of("toLowerCase", UnnecessaryCaseChange::isLowerCase, "toUpperCase", UnnecessaryCaseChange::isUpperCase);

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UnnecessaryCaseChange");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#unnecessarycasechange";
	}

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof MethodCallExpr)) {
			return false;
		}

		var methodCall = (MethodCallExpr) node.getNode();
		var methodName = methodCall.getNameAsString();
		if (!("equals".equals(methodName) || "equalsIgnoreCase".equals(methodName)) || methodCall.getArguments().size() != 1) {
			return false;
		}

		var scope = methodCall.getScope().orElse(null);
		if (!(scope instanceof MethodCallExpr)) {
			return false;
		}

		var innerCall = (MethodCallExpr) scope;
		var checker = CASE_CHECKERS.get(innerCall.getNameAsString());
		if (checker == null || !innerCall.getArguments().isEmpty()) {
			return false;
		}

		var scopeExpr = innerCall.getScope().orElse(null);
		if (!isString(scopeExpr)) {
			return false;
		}

		var argument = methodCall.getArgument(0);
		if (argument.isStringLiteralExpr()) {
			var literalValue = argument.asStringLiteralExpr().asString();

			if (!checker.test(literalValue)) {
				return false;
			}
		}

		var replacement = new MethodCallExpr(scopeExpr.clone(),
				"equalsIgnoreCase",
				NodeList.nodeList(methodCall.getArguments().get(0).clone()));

		return tryReplace(methodCall, replacement);
	}

	private static boolean isString(Expression expression) {
		return expression != null && "java.lang.String".equals(expression.calculateResolvedType().describe());
	}

	@SuppressWarnings({ "PMD.UnnecessaryCaseChange", "PMD.UseLocaleWithCaseConversions" })
	private static boolean isLowerCase(@Nonnull String string) {
		return string.toLowerCase().equals(string);
	}

	@SuppressWarnings({ "PMD.UnnecessaryCaseChange", "PMD.UseLocaleWithCaseConversions" })
	private static boolean isUpperCase(@Nonnull String string) {
		return string.toUpperCase().equals(string);
	}
}
