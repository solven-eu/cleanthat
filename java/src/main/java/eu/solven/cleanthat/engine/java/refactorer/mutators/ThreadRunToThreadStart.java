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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 'myThread.run()` into `myThread.start()`
 *
 * @author Benoit Lacelle
 */
public class ThreadRunToThreadStart extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1217");
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Thread");
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isMethodCallExpr()) {
			return false;
		}
		var methodCallExpr = expr.getNode().asMethodCallExpr();

		if (!"run".equals(methodCallExpr.getNameAsString())) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(methodCallExpr.getScope()),
				Thread.class)) {
			return false;
		}

		return tryReplace(methodCallExpr,
				new MethodCallExpr(methodCallExpr.getScope().get(), "start", methodCallExpr.getArguments()));
	}
}
