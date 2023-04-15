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

import com.github.javaparser.ast.expr.Expression;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `new String("StringLiteral")` into `"StringLiteral"`
 *
 * @author Benoit Lacelle
 */
public class StringFromString extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1DOT1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("String");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2129");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("RemoveNewStringConstructor");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/remove-new-string-constructor.html";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isObjectCreationExpr()) {
			return false;
		}

		var methodCall = expr.asObjectCreationExpr();

		if (!scopeHasRequiredType(Optional.of(methodCall), String.class)) {
			return false;
		} else if (methodCall.getArguments().size() != 1) {
			return false;
		}

		Optional<Expression> optStringExpr = findStringExpr(methodCall.getArgument(0));
		if (optStringExpr.isEmpty()) {
			return false;
		}

		return tryReplace(expr, optStringExpr.get());
	}

	private Optional<Expression> findStringExpr(Expression argument) {
		while (argument.isEnclosedExpr()) {
			argument = argument.asEnclosedExpr().getInner();
		}

		if (!scopeHasRequiredType(Optional.of(argument), String.class)) {
			return Optional.empty();
		}

		return Optional.of(argument);
	}
}
