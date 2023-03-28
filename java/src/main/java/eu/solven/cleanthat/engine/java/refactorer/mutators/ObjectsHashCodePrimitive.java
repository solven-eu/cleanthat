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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Turns 'Object.hashCode(1)` into `Integer.hashCode(1)`
 *
 * @author Benoit Lacelle
 */
// https://errorprone.info/bugpattern/ObjectsHashCodePrimitive
public class ObjectsHashCodePrimitive extends AJavaparserExprMutator {
	private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASSES = ImmutableMap.<Class<?>, Class<?>>builder()
			.put(int.class, Integer.class)
			.put(long.class, Long.class)
			.put(float.class, Float.class)
			.put(double.class, Double.class)
			.build();

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive");
	}

	@Override
	public Optional<String> getErrorProneId() {
		return Optional.of("ObjectsHashCodePrimitive");
	}

	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}
		var methodCall = expr.asMethodCallExpr();
		var methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!"hashCode".equals(methodCallIdentifier)) {
			return false;
		}

		if (methodCall.getArguments().size() != 1) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();

		if (optScope.isEmpty() || !optScope.get().isNameExpr()
				|| !"Objects".equals(optScope.get().asNameExpr().getNameAsString())) {
			return false;
		}

		var left = methodCall.getArgument(0);

		Optional<Entry<Class<?>, Class<?>>> optClass = PRIMITIVE_CLASSES.entrySet()
				.stream()
				.filter(c -> scopeHasRequiredType(Optional.of(left), c.getKey()))
				.findAny();
		if (optClass.isPresent()) {
			// Beware, there may already a custom `Integer` class in imports conflicting with `java.lang.Integer`
			var replacement = new MethodCallExpr(new NameExpr(optClass.get().getValue().getSimpleName()),
					"hashCode",
					new NodeList<>(left));
			return tryReplace(expr, replacement);
		} else {
			return false;
		}
	}
}
