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
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns 'd == Double.NaN' into 'Double.isNaN(d)'
 *
 * @author Benoit Lacelle
 */
public class ComparisonWithNaN extends AJavaparserMutator {

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
		return "https://pmd.github.io/latest/pmd_rules_java_errorprone.html#comparisonwithnan";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof BinaryExpr)) {
			return false;
		}
		var binaryExpr = (BinaryExpr) node;

		if (isEquals(binaryExpr)) {
			return false;
		}

		var left = binaryExpr.getLeft();
		var right = binaryExpr.getRight();

		Expression mayNotBeNaN;

		if (isNaNReference(right)) {
			mayNotBeNaN = left;
		} else if (isNaNReference(left)) {
			mayNotBeNaN = right;
		} else {
			return false;
		}

		boolean prefixNullCheck;
		Class<?> methodHolderClass;
		if (scopeHasRequiredType(Optional.of(mayNotBeNaN), float.class)) {
			prefixNullCheck = false;
			methodHolderClass = Float.class;
		} else if (scopeHasRequiredType(Optional.of(mayNotBeNaN), double.class)) {
			prefixNullCheck = false;
			methodHolderClass = Double.class;
		} else if (scopeHasRequiredType(Optional.of(mayNotBeNaN), Float.class)) {
			prefixNullCheck = true;
			methodHolderClass = Float.class;
		} else if (scopeHasRequiredType(Optional.of(mayNotBeNaN), Double.class)) {
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

		return node.replace(replacement);

	}

	// https://github.com/pmd/pmd/issues/2716
	@SuppressWarnings("PMD.CompareObjectsWithEquals")
	private boolean isEquals(BinaryExpr binaryExpr) {
		return binaryExpr.getOperator() != BinaryExpr.Operator.EQUALS;
	}

	private boolean isNaNReference(Expression left) {
		if (!(left.isFieldAccessExpr())) {
			return false;
		}

		var fieldAccessExpr = left.asFieldAccessExpr();

		if (!("NaN".equals(fieldAccessExpr.getNameAsString()))) {
			return false;
		}
		if (!scopeHasRequiredType(Optional.of(fieldAccessExpr.getScope()), Double.class)
				&& !scopeHasRequiredType(Optional.of(fieldAccessExpr.getScope()), Float.class)) {
			return false;
		}

		return true;
	}
}
