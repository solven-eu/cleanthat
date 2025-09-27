/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns `Integer i = Integer.valueOf(2)` into `Integer i = 2`
 * 
 * Turns `String i = Integer.valueOf(2).toString()` into `Integer i = Integer.toString(2)`
 *
 * @author Benoit Lacelle
 */
@Slf4j
public class UnnecessaryBoxing extends AJavaparserNodeMutator {

	@Override
	public boolean isDraft() {
		return false;
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1DOT1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/primitive-boxed-for-string.html";
	}

	@Override
	public String pmdUrl() {
		// "https://pmd.github.io/pmd-doc-6.55.0/pmd_rules_java_performance.html#unnecessarywrapperobjectcreation";
		return "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#unnecessaryboxing";
	}

	@Override
	public Set<String> getPmdIds() {
		return Set.of("UnnecessaryBoxing", "UnnecessaryWrapperObjectCreation");
	}

	@Override
	public Optional<String> getSonarId() {
		// Relates with "RSPEC-2153"
		return Optional.of("RSPEC-1158");
	}

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		var transformed = new AtomicBoolean();

		onMethodName(nodeAndSymbolSolver, "toString", (methodNode, scope, type) -> {
			if (process(nodeAndSymbolSolver.editNode(methodNode), scope, type)) {
				transformed.set(true);
			}
		});

		return transformed.get();
	}

	@SuppressWarnings({ "PMD.AvoidDeeplyNestedIfStmts", "PMD.CognitiveComplexity" })
	private boolean process(NodeAndSymbolSolver<?> nodeAndSymbolSolver, Expression scope, ResolvedType type) {
		if (!type.isReferenceType()) {
			return false;
		}
		LOGGER.debug("{} is referenceType", type);

		var primitiveQualifiedName = type.asReferenceType().getQualifiedName();
		if (Boolean.class.getName().equals(primitiveQualifiedName)
				|| Byte.class.getName().equals(primitiveQualifiedName)
				|| Short.class.getName().equals(primitiveQualifiedName)
				|| Integer.class.getName().equals(primitiveQualifiedName)
				|| Long.class.getName().equals(primitiveQualifiedName)
				|| Float.class.getName().equals(primitiveQualifiedName)
				|| Double.class.getName().equals(primitiveQualifiedName)) {
			LOGGER.debug("{} is AutoBoxed", type);
			if (scope instanceof ObjectCreationExpr) {
				// new Boolean(b).toString()
				var creation = (ObjectCreationExpr) scope;
				NodeList<Expression> inputs = creation.getArguments();
				var replacement = new MethodCallExpr(new NameExpr(creation.getType().getName()), "toString", inputs);
				return tryReplace(nodeAndSymbolSolver.getNode(), replacement);
			} else if (scope instanceof MethodCallExpr) {
				// Boolean.valueOf(b).toString()
				var call = (MethodCallExpr) scope;

				if (!"valueOf".equals(call.getNameAsString()) || call.getScope().isEmpty()) {
					return false;
				}

				var calledScope = call.getScope().get();
				Optional<ResolvedType> calledType =
						MethodCallExprHelpers.optResolvedType(nodeAndSymbolSolver.editNode(calledScope));

				if (calledType.isEmpty() || !calledType.get().isReferenceType()) {
					return false;
				}

				var referenceType = calledType.get().asReferenceType();

				if (referenceType.hasName() && primitiveQualifiedName.equals(referenceType.getQualifiedName())) {
					var replacement = new MethodCallExpr(calledScope, "toString", call.getArguments());

					return tryReplace(nodeAndSymbolSolver.getNode(), replacement);
				}
			}
		}

		return false;
	}
}
