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
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `intStream.forEach(j -> { int c = s.charAt(j); sb.append(c); });`
 * 
 * into `intStream.map(j -> s.charAt(j)).forEach(c -> sb.append(c))`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
public class StreamWrappedVariableToMap extends OptionalWrappedVariableToMap {

	private static final String METHOD_MAP = "map";
	private static final String METHOD_MAP_TO_INT = "mapToInt";
	private static final String METHOD_MAP_TO_LNG = "mapToLong";
	private static final String METHOD_MAP_TO_DBL = "mapToDouble";

	// These methods are turned into a plain `.map` by adding a prefix `.map[XXX]`
	private static final Set<String> SIMPLIFIED_TO_MAP =
			Set.of(METHOD_MAP_TO_INT, METHOD_MAP_TO_LNG, METHOD_MAP_TO_DBL);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive", "Loop", "Stream");
	}

	@Override
	public Set<String> getLegacyIds() {
		return Set.of("SimplifyStreamVariablesWithMap");
	}

	@Override
	protected Set<String> getEligibleForUnwrappedMap() {
		return Set.of("forEach", METHOD_MAP, METHOD_MAP_TO_INT, METHOD_MAP_TO_LNG, METHOD_MAP_TO_DBL);
	}

	@Override
	protected Class<?> getExpectedScope() {
		return BaseStream.class;
	}

	@Override
	protected Optional<String> computeMapMethodName(Expression expression, Type type) {
		assert scopeHasRequiredType(Optional.of(expression), BaseStream.class);

		if (type.isPrimitiveType()) {
			Primitive primitiveType = type.asPrimitiveType().getType();
			if (scopeHasRequiredType(Optional.of(expression), IntStream.class)
					&& primitiveType == PrimitiveType.Primitive.INT
					|| scopeHasRequiredType(Optional.of(expression), LongStream.class)
							&& primitiveType == PrimitiveType.Primitive.LONG
					|| scopeHasRequiredType(Optional.of(expression), DoubleStream.class)
							&& primitiveType == PrimitiveType.Primitive.DOUBLE) {
				return Optional.of(METHOD_MAP);
			} else if (primitiveType == PrimitiveType.Primitive.INT) {
				return Optional.of(METHOD_MAP_TO_INT);
			} else if (primitiveType == PrimitiveType.Primitive.LONG) {
				return Optional.of(METHOD_MAP_TO_LNG);
			} else if (primitiveType == PrimitiveType.Primitive.DOUBLE) {
				return Optional.of(METHOD_MAP_TO_DBL);
			}
		} else if (scopeHasRequiredType(Optional.of(expression), Object.class)) {
			if (scopeHasRequiredType(Optional.of(expression), Stream.class)) {
				// Object and Stream
				return Optional.of(METHOD_MAP);
			} else {
				// Object and Stream
				return Optional.of("mapToObj");
			}
		}

		return Optional.empty();
	}

	@Override
	protected void adjustMethodName(MethodCallExpr methodCallExpr) {
		if (SIMPLIFIED_TO_MAP.contains(methodCallExpr.getNameAsString())) {
			methodCallExpr.setName(METHOD_MAP);
		}
	}
}
