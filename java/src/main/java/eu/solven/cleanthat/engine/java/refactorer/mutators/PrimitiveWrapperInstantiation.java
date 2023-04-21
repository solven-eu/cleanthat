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
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ResolvedTypeHelpers;

/**
 * Turns `new Double(d)` into `Double.valueOf(d)`
 *
 * @author Benoit Lacelle
 */
public class PrimitiveWrapperInstantiation extends AJavaparserExprMutator {
	@Override
	public String minimalJavaVersion() {
		// java.lang.Boolean.valueOf(boolean): 1.4
		// java.lang.Double.valueOf(double): 1.5
		return IJdkVersionConstants.JDK_5;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	public Set<String> getLegacyIds() {
		return Set.of("BoxedPrimitiveConstructor");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("PrimitiveWrapperInstantiation");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#primitivewrapperinstantiation";
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		if (!expr.getNode().isObjectCreationExpr()) {
			return false;
		}

		var objectCreationExpr = expr.getNode().asObjectCreationExpr();
		if (objectCreationExpr.getArguments().size() != 1) {
			return false;
		}

		var type = objectCreationExpr.getType();

		if (!isBoxType(type)) {
			return false;
		}

		MethodCallExpr newMethodCall =
				new MethodCallExpr(new NameExpr(type.getName()), "valueOf", objectCreationExpr.getArguments());
		return tryReplace(objectCreationExpr, newMethodCall);
	}

	private boolean isBoxType(ClassOrInterfaceType type) {
		// We check the scope as a workaround to https://github.com/javaparser/javaparser/issues/3968
		if (type.isBoxedType()) {
			// In fact, it may not be a real Boxed type
			Optional<ResolvedType> optResolvedType = ResolvedTypeHelpers.optResolvedType(type);
			if (optResolvedType.isEmpty()) {
				return false;
			}
			return ResolvedPrimitiveType.isBoxType(optResolvedType.get());
		} else {
			// This is definitely not a boxedType
			return false;
		}
	}
}
