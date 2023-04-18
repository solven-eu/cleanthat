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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;
import eu.solven.cleanthat.engine.java.refactorer.meta.IReApplyUntilNoop;
import eu.solven.cleanthat.engine.java.refactorer.meta.RepeatOnSuccess;

/**
 * Turns `stream.forEach(value -> { value.forEach(user -> { System.out.println(user); }); });`
 * 
 * into `stream.flatMap(value -> value.stream()).forEach(user -> { System.out.println(user); });`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe({ LambdaIsMethodReference.class })
@RepeatOnSuccess
public class StreamForEachNestingForLoopToFlatMap extends AJavaparserExprMutator implements IReApplyUntilNoop {
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamForEachNestingForLoopToFlatMap.class);

	static final String ANY_MATCH = "anyMatch";

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
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}

		var callForEach = expr.asMethodCallExpr();
		if (!"forEach".equals(callForEach.getNameAsString())) {
			return false;
		} else if (callForEach.getArguments().size() != 1) {
			return false;
		}

		Expression ensureStream;
		if (scopeHasRequiredType(callForEach.getScope(), Stream.class)) {
			ensureStream = callForEach.getScope().get();
		} else if (scopeHasRequiredType(callForEach.getScope(), Collection.class)) {
			ensureStream = new MethodCallExpr(callForEach.getScope().get(), "stream");
		} else {
			return false;
		}

		Expression forEachArgument = callForEach.getArgument(0);
		if (!forEachArgument.isLambdaExpr()) {
			return false;
		}

		LambdaExpr forEachLambdaExpr = forEachArgument.asLambdaExpr();

		Optional<MethodCallExpr> optSingleMethodCall =
				StreamMutatorHelpers.findSingleMethodCallExpr(forEachLambdaExpr.getBody());
		if (optSingleMethodCall.isEmpty()) {
			return false;
		}

		if (!"forEach".equals(optSingleMethodCall.get().getNameAsString())) {
			return false;
		}

		Expression flatMapExpression = optSingleMethodCall.get().getScope().get();

		if (scopeHasRequiredType(Optional.of(flatMapExpression), Stream.class)) {
			LOGGER.debug("We are flattening into something like `.flatMap(s -> s)`");
		} else if (scopeHasRequiredType(Optional.of(flatMapExpression), Collection.class)) {
			flatMapExpression = new MethodCallExpr(flatMapExpression, "stream");
		} else {
			return false;
		}

		Optional<LambdaExpr> flatMapLambdaExpr =
				LambdaExprHelpers.makeLambdaExpr(forEachLambdaExpr.getParameter(0).getName(), flatMapExpression);
		if (flatMapLambdaExpr.isEmpty()) {
			return false;
		}

		MethodCallExpr callFlatMap =
				new MethodCallExpr(ensureStream, "flatMap", new NodeList<>(flatMapLambdaExpr.get()));
		MethodCallExpr callInnerForEach =
				new MethodCallExpr(callFlatMap, "forEach", optSingleMethodCall.get().getArguments());

		return tryReplace(callForEach, callInnerForEach);
	}
}
