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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns '"someString".toString()' into '"someString"'
 *
 * @author Benoit Lacelle
 */
public class StringToString extends AJavaparserMutator {
	private static final String METHOD_TO_STRING = "toString";

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("StringToString");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#stringtostring";
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("RemoveToStringOnString");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/remove-to-string-on-string.html";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		var methodCall = (MethodCallExpr) node;
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!METHOD_TO_STRING.equals(methodCallIdentifier)) {
			return false;
		}
		Optional<Node> optParent = methodCall.getParentNode();
		if (methodCall.getScope().isEmpty() || optParent.isEmpty()) {
			return false;
		}
		Optional<Expression> optScope = methodCall.getScope();

		if (!scopeHasRequiredType(optScope, String.class)) {
			return false;
		}

		var scope = optScope.get();
		return tryReplace(node, scope);
	}
}
