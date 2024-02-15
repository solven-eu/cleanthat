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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.config.pojo.ICleanthatStepParametersProperties;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;

/**
 * Turns 'ImmutableMap.of("k1", 0, "k2", 1)` into `ImmutableMap.builder().put("k1", 0).put("k2", 1).build()`
 *
 * @author Benoit Lacelle
 */
public class GuavaImmutableMapBuilderOverVarargs extends AJavaparserExprMutator {
	// key and value
	private static final int ARG_PER_ENTRY = 2;

	// We do not convert a single entry to builder: ImmutableMap.of("k1", 0) is very fine
	private static final int MINIMAL_ENTRY_TO_TRIGGER = 2;

	// Broken due to https://github.com/javaparser/javaparser/issues/3976
	// TODO This needs to detect when generics are necessary or not
	private static final boolean CAN_MANAGE_GENERIC_TYPES = false;

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of(ICleanthatStepParametersProperties.GUAVA, "Varargs");
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.getNode().asMethodCallExpr();
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!"of".equals(methodCallIdentifier)) {
			return false;
		} else if (methodCall.getScope().isEmpty()) {
			return false;
		}

		// We check this is compatible with key-value vararg
		if (methodCall.getArguments().size() % ARG_PER_ENTRY != 0) {
			return false;
		} else if (methodCall.getArguments().size() < ARG_PER_ENTRY * MINIMAL_ENTRY_TO_TRIGGER) {
			// We keep `ImmutableMap.of()` and `ImmutableMap.of("k", "v")` as is
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();

		if (optScope.isEmpty() || !optScope.get().isNameExpr()
				|| !MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(optScope), ImmutableMap.class)) {
			return false;
		}

		MethodCallExpr builder = new MethodCallExpr(optScope.get(), "builder");

		if (CAN_MANAGE_GENERIC_TYPES) {
			// In many cases, we need to add type arguments as the compiler can not infer them by itself
			// https://github.com/google/guava/issues/883
			// https://github.com/google/guava/issues/1166
			// https://github.com/javaparser/javaparser/issues/2135#issuecomment-1094295844
			TypeSolver typeSolver = JavaRefactorer.makeDefaultTypeSolver(true);
			JavaParserFacade jpf = JavaParserFacade.get(typeSolver);
			MethodUsage methodUsage = jpf.solveMethodAsUsage(methodCall);
			List<ResolvedType> types = methodUsage.getParamTypes();

			// JavaParser.parseClassOrInterfaceType
			ClassOrInterfaceType fullyQualifiedKeyType =
					new ClassOrInterfaceType(types.get(0).asReferenceType().getId());
			ClassOrInterfaceType fullyQualifiedValueType =
					new ClassOrInterfaceType(types.get(1).asReferenceType().getId());

			builder.setTypeArguments(fullyQualifiedKeyType, fullyQualifiedValueType);
		}

		for (var i = 0; i < methodCall.getArguments().size() / 2; i++) {
			builder = new MethodCallExpr(builder,
					"put",
					new NodeList<>(methodCall.getArgument(2 * i), methodCall.getArgument(2 * i + 1)));
		}

		return tryReplace(methodCall, new MethodCallExpr(builder, "build"));
	}
}
