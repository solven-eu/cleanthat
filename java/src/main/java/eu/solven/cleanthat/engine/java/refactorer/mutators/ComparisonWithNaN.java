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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 'd == Double.NaN' into 'Double.isNaN(d)'
 *
 * @author Benoit Lacelle
 */
public class ComparisonWithNaN extends AJavaparserNodeMutator {

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("ComparisonWithNaN");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_errorprone.html#comparisonwithnan";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof BinaryExpr)) {
			return false;
		}
		var binaryExpr = (BinaryExpr) node.getNode();

		if (isEquals(binaryExpr)) {
			return false;
		}

		var left = binaryExpr.getLeft();
		var right = binaryExpr.getRight();

		Expression mayNotBeNaN;

		if (isNaNReference(node.editNode(right))) {
			mayNotBeNaN = left;
		} else if (isNaNReference(node.editNode(left))) {
			mayNotBeNaN = right;
		} else {
			return false;
		}

		boolean prefixNullCheck;
		Class<?> methodHolderClass;
		if (MethodCallExprHelpers.scopeHasRequiredType(node.editNode(mayNotBeNaN), float.class)) {
			prefixNullCheck = false;
			methodHolderClass = Float.class;
		} else if (MethodCallExprHelpers.scopeHasRequiredType(node.editNode(mayNotBeNaN), double.class)) {
			prefixNullCheck = false;
			methodHolderClass = Double.class;
		} else if (MethodCallExprHelpers.scopeHasRequiredType(node.editNode(mayNotBeNaN), Float.class)) {
			prefixNullCheck = true;
			methodHolderClass = Float.class;
		} else if (MethodCallExprHelpers.scopeHasRequiredType(node.editNode(mayNotBeNaN), Double.class)) {
			prefixNullCheck = true;
			methodHolderClass = Double.class;
		} else {
			return false;
		}
		var nameExpr = new NameExpr(methodHolderClass.getSimpleName());
		var properNaNCall = new MethodCallExpr(nameExpr, "isNaN", new NodeList<>(mayNotBeNaN));

		Node replacement;
		if (prefixNullCheck) {
			var notNull = new BinaryExpr(mayNotBeNaN, new NullLiteralExpr(), Operator.NOT_EQUALS);
			replacement = new BinaryExpr(notNull, properNaNCall, Operator.AND);
		} else {
			replacement = properNaNCall;
		}

		return tryReplace(node, replacement);

	}

	// https://github.com/pmd/pmd/issues/2716
	@SuppressWarnings("PMD.CompareObjectsWithEquals")
	private boolean isEquals(BinaryExpr binaryExpr) {
		return binaryExpr.getOperator() != BinaryExpr.Operator.EQUALS;
	}

	private boolean isNaNReference(NodeAndSymbolSolver<? extends Expression> left) {
		if (!(left.getNode().isFieldAccessExpr())) {
			return false;
		}

		var fieldAccessExpr = left.getNode().asFieldAccessExpr();

		if (!("NaN".equals(fieldAccessExpr.getNameAsString()))) {
			return false;
		}
		if (!MethodCallExprHelpers.scopeHasRequiredType(left.editNode(fieldAccessExpr.getScope()), Double.class)
				&& !MethodCallExprHelpers.scopeHasRequiredType(left.editNode(fieldAccessExpr.getScope()),
						Float.class)) {
			return false;
		}

		return true;
	}
}
