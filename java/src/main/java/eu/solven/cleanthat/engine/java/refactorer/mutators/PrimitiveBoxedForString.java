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
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IRuleExternalReferences;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Clean the way of converting primitives into {@link String}.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/primitive-boxed-for-string.html
// https://rules.sonarsource.com/java/RSPEC-1158
public class PrimitiveBoxedForString extends AJavaParserMutator implements IMutator, IRuleExternalReferences {
	private static final Logger LOGGER = LoggerFactory.getLogger(PrimitiveBoxedForString.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1DOT1;
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/reorder-modifiers.html";
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#unnecessarywrapperobjectcreation";
	}

	@Override
	public Optional<String> getPmdId() {
		// This matches multiple CleanThat rules
		return Optional.of("UnnecessaryWrapperObjectCreation");
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		AtomicBoolean transformed = new AtomicBoolean();

		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		onMethodName(node, "toString", (methodNode, scope, type) -> {
			if (process(methodNode, scope, type)) {
				transformed.set(true);
			}
		});

		return transformed.get();
	}

	@SuppressWarnings({ "PMD.AvoidDeeplyNestedIfStmts", "PMD.CognitiveComplexity" })
	private boolean process(Node node, Expression scope, ResolvedType type) {
		if (!type.isReferenceType()) {
			return false;
		}
		LOGGER.debug("{} is referenceType", type);
		String primitiveQualifiedName = type.asReferenceType().getQualifiedName();
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
				ObjectCreationExpr creation = (ObjectCreationExpr) scope;
				NodeList<Expression> inputs = creation.getArguments();
				MethodCallExpr replacement =
						new MethodCallExpr(new NameExpr(creation.getType().getName()), "toString", inputs);
				LOGGER.info("Turning {} into {}", node, replacement);
				return node.replace(replacement);
			} else if (scope instanceof MethodCallExpr) {
				// Boolean.valueOf(b).toString()
				MethodCallExpr call = (MethodCallExpr) scope;

				if (!"valueOf".equals(call.getNameAsString()) || call.getScope().isEmpty()) {
					return false;
				}

				Expression calledScope = call.getScope().get();
				Optional<ResolvedType> calledType = optResolvedType(calledScope);

				if (calledType.isEmpty() || !calledType.get().isReferenceType()) {
					return false;
				}

				ResolvedReferenceType referenceType = calledType.get().asReferenceType();

				if (referenceType.hasName() && primitiveQualifiedName.equals(referenceType.getQualifiedName())) {
					MethodCallExpr replacement = new MethodCallExpr(calledScope, "toString", call.getArguments());

					return node.replace(replacement);
				}
			}
		}

		return false;
	}
}
