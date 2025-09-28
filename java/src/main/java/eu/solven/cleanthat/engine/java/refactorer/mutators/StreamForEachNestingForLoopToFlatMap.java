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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;
import eu.solven.cleanthat.engine.java.refactorer.meta.IReApplyUntilNoop;
import eu.solven.cleanthat.engine.java.refactorer.meta.RepeatOnSuccess;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns `stream.forEach(value -> { value.forEach(user -> { System.out.println(user); }); });`
 * 
 * into `stream.flatMap(value -> value.stream()).forEach(user -> { System.out.println(user); });`
 *
 * @author Benoit Lacelle
 */
@Slf4j
@ApplyAfterMe({ LambdaIsMethodReference.class })
@RepeatOnSuccess
public class StreamForEachNestingForLoopToFlatMap extends AJavaparserExprMutator implements IReApplyUntilNoop {

	private static final Map<Class<?>, String> TYPE_TO_FLATMAP = Map.of(Stream.class,
			"flatMap",
			IntStream.class,
			"flatMapToInt",
			LongStream.class,
			"flatMapToLong",
			DoubleStream.class,
			"flatMapToDouble");

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream", "Loop");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("FlatMapInsteadOfNestedLoops");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/flat-map-instead-of-nested-loops.html";
	}

	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> expr) {
		Optional<MethodCallExpr> optMethodCall =
				MethodCallExprHelpers.match(expr, Object.class, "forEach", e -> e.isLambdaExpr());
		if (optMethodCall.isEmpty()) {
			return false;
		}

		var callForEach = optMethodCall.get();

		Expression ensureStream;
		if (MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(callForEach.getScope()), Stream.class)) {
			ensureStream = callForEach.getScope().get();
		} else if (MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(callForEach.getScope()),
				Collection.class)) {
			ensureStream = new MethodCallExpr(callForEach.getScope().get(), "stream");
		} else {
			return false;
		}

		LambdaExpr forEachLambdaExpr = callForEach.getArgument(0).asLambdaExpr();

		Optional<MethodCallExpr> optSingleMethodCall =
				StreamMutatorHelpers.findSingleMethodCallExpr(forEachLambdaExpr.getBody());
		if (optSingleMethodCall.isEmpty()) {
			return false;
		}

		MethodCallExpr singleMethodCall = optSingleMethodCall.get();
		if (!"forEach".equals(singleMethodCall.getNameAsString())) {
			return false;
		}

		Expression rawFlatMapExpression = singleMethodCall.getScope().get();

		Expression flatMapExpression;
		Optional<String> flatMapMethod;
		if (MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(rawFlatMapExpression), Collection.class)) {
			// We clone as creating a `MethodCallExpr` will change the parent, and the whole mutator can still be
			// cancelled
			flatMapExpression = new MethodCallExpr(rawFlatMapExpression.clone(), "stream");
			flatMapMethod = Optional.of("flatMap");
		} else {
			LOGGER.debug("We are flattening into something like `.flatMap(s -> s)`");
			flatMapExpression = rawFlatMapExpression;
			flatMapMethod = TYPE_TO_FLATMAP.entrySet()
					.stream()
					.filter(e -> MethodCallExprHelpers.scopeHasRequiredType(expr.editNode(rawFlatMapExpression),
							e.getKey()))
					.map(Map.Entry::getValue)
					.findAny();
		}

		if (flatMapMethod.isEmpty()) {
			return false;
		}

		Optional<LambdaExpr> flatMapLambdaExpr =
				LambdaExprHelpers.makeLambdaExpr(forEachLambdaExpr.getParameter(0).getName(), flatMapExpression);
		if (flatMapLambdaExpr.isEmpty()) {
			return false;
		}

		MethodCallExpr callFlatMap =
				new MethodCallExpr(ensureStream, flatMapMethod.get(), new NodeList<>(flatMapLambdaExpr.get()));
		MethodCallExpr callInnerForEach = new MethodCallExpr(callFlatMap, "forEach", singleMethodCall.getArguments());

		return tryReplace(callForEach, callInnerForEach);
	}
}
