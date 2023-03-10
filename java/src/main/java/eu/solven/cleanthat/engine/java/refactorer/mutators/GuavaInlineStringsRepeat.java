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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns 'Strings.repeat("abc", 3)` into `"abc".repeat(3)`
 *
 * @author Benoit Lacelle
 */
// https://errorprone.info/docs/inlineme
// see com.google.common.base.Strings.repeat(String, int)
public class GuavaInlineStringsRepeat extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public Optional<String> getErrorProneId() {
		return Optional.of("InlineMeInliner");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.asMethodCallExpr();
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!"repeat".equals(methodCallIdentifier)) {
			return false;
		}

		if (methodCall.getArguments().size() != 2) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();

		if (optScope.isEmpty() || !optScope.get().isNameExpr()
				|| !optScope.get().asNameExpr().getNameAsString().equals("Strings")) {
			return false;
		}

		Expression repeatedString = methodCall.getArgument(0);
		MethodCallExpr replacement =
				new MethodCallExpr(repeatedString, "repeat", new NodeList<>(methodCall.getArgument(1)));
		return tryReplace(expr, replacement);
	}
}
