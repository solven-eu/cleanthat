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
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Prevent relying .equals on {@link Enum} types
 *
 * @author Benoit Lacelle
 */
// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
// https://stackoverflow.com/questions/1750435/comparing-java-enum-members-or-equals
public class EnumsWithoutEquals extends AJavaparserMutator {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnumsWithoutEquals.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of();
	}

	@Override
	public String getId() {
		return "EnumsWithoutEquals";
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/enums-without-equals.html";
	}

	// https://stackoverflow.com/questions/55309460/how-to-replace-expression-by-string-in-javaparser-ast
	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		var mutated = new AtomicBoolean(false);
		onMethodName(node, "equals", (methodCall, scope, type) -> {
			if (type.isReferenceType()) {
				var isEnum = false;
				var referenceType = type.asReferenceType();

				referenceType.isJavaLangEnum();

				var className = referenceType.getQualifiedName();

				try {
					Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

					isEnum = Enum.class.isAssignableFrom(clazz);
				} catch (ClassNotFoundException e) {
					LOGGER.debug("Class is not available", e);
				}

				if (isEnum && methodCall.getArguments().size() == 1) {
					var singleArgument = methodCall.getArgument(0);

					Optional<Node> optParentNode = methodCall.getParentNode();

					boolean isNegated;
					if (optParentNode.isPresent()) {
						var parent = optParentNode.get();

						if (parent instanceof UnaryExpr
								&& ((UnaryExpr) parent).getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
							isNegated = true;
						} else {
							isNegated = false;
						}
					} else {
						isNegated = false;
					}

					if (isNegated) {
						var replacement = new BinaryExpr(scope, singleArgument, BinaryExpr.Operator.NOT_EQUALS);

						if (tryReplace(optParentNode.get(), replacement)) {
							mutated.set(true);
						}
					} else {
						var replacement = new BinaryExpr(scope, singleArgument, BinaryExpr.Operator.EQUALS);

						if (tryReplace(node, replacement)) {
							mutated.set(true);
						}
					}
				}
			}
		});

		return mutated.get();
	}

}
