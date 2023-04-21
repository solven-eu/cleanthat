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
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Helps working with {@link MethodCallExpr}
 * 
 * @author Benoit Lacelle
 *
 */
public class MethodCallExprHelpers {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodCallExprHelpers.class);

	protected MethodCallExprHelpers() {
		// hidden
	}

	public static boolean scopeHasRequiredType(NodeAndSymbolSolver<? extends Expression> node, Class<?> requiredType) {
		return scopeHasRequiredType(node.getSymbolResolver(), Optional.of(node.getNode()), requiredType);
	}

	public static boolean scopeHasRequiredType(Optional<NodeAndSymbolSolver<? extends Expression>> optNode,
			Class<?> expectedScope) {
		if (optNode.isEmpty()) {
			return false;
		}
		return scopeHasRequiredType(optNode.get(), expectedScope);
	}

	public static boolean scopeHasRequiredType(SymbolResolver symbolResolver,
			Optional<Expression> optScope,
			Class<?> requiredType) {
		// .canonicalName is typically required to handle primitive arrays
		// For int[]: qualifiedName is [I while canonicalName is int[]
		// BEWARE: How would it handle local or anonymous class?
		// https://stackoverflow.com/questions/5032898/how-to-instantiate-class-class-for-a-primitive-type
		return scopeHasRequiredType(symbolResolver, optScope, requiredType.getName());
	}

	public static boolean scopeHasRequiredType(SymbolResolver symbolResolver,
			Optional<Expression> optScope,
			String requiredType) {
		Optional<ResolvedType> optType =
				optScope.flatMap(scope -> MethodCallExprHelpers.optResolvedType(symbolResolver, scope));

		return ResolvedTypeHelpers.typeIsAssignable(optType, requiredType);
	}

	public static Optional<ResolvedType> optResolvedType(NodeAndSymbolSolver<? extends Expression> expr) {
		return optResolvedType(expr.getSymbolResolver(), expr.getNode());
	}

	public static Optional<ResolvedType> optResolvedType(Optional<NodeAndSymbolSolver<? extends Expression>> optExpr) {
		if (optExpr.isEmpty()) {
			return Optional.empty();
		}
		return optResolvedType(optExpr.get());
	}

	// https://github.com/javaparser/javaparser/issues/1491
	public static Optional<ResolvedType> optResolvedType(SymbolResolver symbolResolver, Expression expr) {
		try {
			// ResolvedType type = expr.getSymbolResolver().calculateType(expr);
			var type = symbolResolver.calculateType(expr);
			return Optional.of(type);
		} catch (RuntimeException e) {
			try {
				var secondTryType = expr.calculateResolvedType();

				AJavaparserNodeMutator
						.logJavaParserIssue(expr, e, "https://github.com/javaparser/javaparser/issues/3939");

				return Optional.of(secondTryType);
			} catch (RuntimeException ee) {
				LOGGER.debug("Issue resolving the type of {}", expr, ee);
				return Optional.empty();
			} catch (NoClassDefFoundError ee) {
				AJavaparserNodeMutator
						.logJavaParserIssue(expr, e, "https://github.com/javaparser/javaparser/issues/3504");

				return Optional.empty();
			}
		} catch (NoClassDefFoundError e) {
			AJavaparserNodeMutator.logJavaParserIssue(expr, e, "https://github.com/javaparser/javaparser/issues/3504");

			return Optional.empty();
		}
	}

	@SafeVarargs
	public static Optional<MethodCallExpr> match(NodeAndSymbolSolver<Expression> expr,
			Class<?> scope,
			String methodName,
			Predicate<Expression>... acceptArguments) {
		if (!expr.getNode().isMethodCallExpr()) {
			return Optional.empty();
		}

		var methodCallExpr = expr.getNode().asMethodCallExpr();
		if (!methodName.equals(methodCallExpr.getNameAsString())) {
			return Optional.empty();
		} else if (methodCallExpr.getArguments().size() != acceptArguments.length) {
			return Optional.empty();
		}

		if (!scopeHasRequiredType(expr.getSymbolResolver(), methodCallExpr.getScope(), scope)) {
			return Optional.empty();
		}

		for (var i = 0; i < acceptArguments.length; i++) {
			Expression flatMapArgument = methodCallExpr.getArgument(i);
			Predicate<Expression> argumentPredicate = acceptArguments[i];
			if (!argumentPredicate.test(flatMapArgument)) {
				return Optional.empty();
			}
		}

		return Optional.of(methodCallExpr);
	}

}
