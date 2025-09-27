/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 's.indexOf("s")’ into ’s.indexOf('s')'.
 *
 * @author Benoit Lacelle
 */
public class UseIndexOfChar extends AJavaparserNodeMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_performance.html#useindexofchar";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseIndexOfChar");
	}

	@Override
	public String sonarUrl() {
		return "https://rules.sonarsource.com/java/RSPEC-1155";
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1155");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/use-is-empty-on-collections.html";
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof StringLiteralExpr)) {
			return false;
		}
		var stringLiteralExpr = (StringLiteralExpr) node.getNode();

		if (stringLiteralExpr.getParentNode().isEmpty()) {
			return false;
		}
		var parentNode = stringLiteralExpr.getParentNode().get();
		if (!(parentNode instanceof MethodCallExpr)) {
			// We search a call for .indexOf
			return false;
		}
		var parentMethodCall = (MethodCallExpr) parentNode;
		var parentMethodAsString = parentMethodCall.getNameAsString();
		var isIndexOf = "indexOf".equals(parentMethodAsString);
		var isLastIndexOf = "lastIndexOf".equals(parentMethodAsString);
		if (!isIndexOf && !isLastIndexOf) {
			// We search a call for .indexOf
			return false;
		}

		Optional<Expression> optScope = parentMethodCall.getScope();
		if (!MethodCallExprHelpers.scopeHasRequiredType(node.editNode(optScope), String.class)) {
			return false;
		}

		var stringLiteralExprValue = stringLiteralExpr.getValue();
		if (stringLiteralExprValue.isEmpty()) {
			if (isIndexOf) {
				return tryReplace(parentNode, new IntegerLiteralExpr("0"));
			} else {
				assert isLastIndexOf;
				var lengthMethodCall = new MethodCallExpr(optScope.get(), "length");
				return tryReplace(parentNode, lengthMethodCall);
			}
		} else if (stringLiteralExprValue.length() != 1) {
			// We consider only String with `.length()==1` to `.indexOf` over the single char
			return false;
		}

		return tryReplace(node, new CharLiteralExpr(stringLiteralExprValue.charAt(0)));
	}
}
