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

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns `optional.map(name -> name)`
 * 
 * into `optional`
 *
 * @author Benoit Lacelle
 */
public class OptionalMapIdentity extends AJavaparserExprMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Optional", "Redundancy");
	}

	protected Class<?> getRequiredType() {
		return Optional.class;
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}
		var mapCall = expr.asMethodCallExpr();
		if (!"map".equals(mapCall.getNameAsString())) {
			return false;
		} else if (mapCall.getArguments().size() != 1) {
			return false;
		} else if (!scopeHasRequiredType(mapCall.getScope(), getRequiredType())) {
			return false;
		}

		if (!mapCall.getArgument(0).isLambdaExpr()) {
			return false;
		}
		var lambdaExpr = mapCall.getArgument(0).asLambdaExpr();

		if (lambdaExpr.getParameters().size() != 1) {
			return false;
		}

		Parameter firstParameter = lambdaExpr.getParameter(0);

		if (lambdaExpr.getExpressionBody().isEmpty()) {
			return false;
		} else if (!lambdaExpr.getExpressionBody().get().equals(firstParameter.getNameAsExpression())) {
			return false;
		}

		return tryReplace(expr, mapCall.getScope().get());
	}
}
