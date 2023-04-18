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
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;
import eu.solven.cleanthat.engine.java.refactorer.meta.IReApplyUntilNoop;
import eu.solven.cleanthat.engine.java.refactorer.meta.RepeatOnSuccess;

/**
 * Turns `s.flatMap(r -> r.stream().filter(c -> c.isEmpty()))`
 * 
 * into `s.flatMap(r -> r.stream()).filter(c -> c.isEmpty())`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe({ LambdaIsMethodReference.class })
@RepeatOnSuccess
public class StreamFlatMapStreamToFlatMap extends AJavaparserExprMutator implements IReApplyUntilNoop {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream");
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
		var optFlatMapExpr = MethodCallExprHelpers.match(expr, Stream.class, "flatMap", Expression::isLambdaExpr);
		if (optFlatMapExpr.isEmpty()) {
			return false;
		}

		var flatMapExpr = optFlatMapExpr.get();
		Optional<Node> optPreviousScope = flatMapExpr.getParentNode();
		Optional<MethodCallExpr> optFlatMapNextMethod = optPreviousScope.flatMap(n -> {
			if (n instanceof MethodCallExpr) {
				return Optional.of((MethodCallExpr) n);
			} else {
				// If there is no call after `.flatMap(XXX)`, the parent would be a ReturnStmt or an AssignExpr
				return Optional.empty();
			}
		});

		if (optFlatMapNextMethod.isEmpty()) {
			// TODO Management this case (e.g. returning the `.flatMap(...)`)
			return false;
		}

		LambdaExpr forEachLambdaExpr = optFlatMapExpr.get().getArgument(0).asLambdaExpr();

		Optional<MethodCallExpr> optFlatMapInnerMethodCall =
				StreamMutatorHelpers.findSingleMethodCallExpr(forEachLambdaExpr.getBody());
		if (optFlatMapInnerMethodCall.isEmpty()) {
			return false;
		}

		MethodCallExpr flatMapInnerMethodCall = optFlatMapInnerMethodCall.get();

		Optional<MethodCallExpr> optInnerToStream = searchInnerToStream(flatMapInnerMethodCall);
		if (optInnerToStream.isEmpty()) {
			return false;
		}
		MethodCallExpr innerToStream = optInnerToStream.get();

		if (!"stream".equals(innerToStream.getNameAsString())) {
			// TODO Here, we should search for the last-ancestor with type `Stream`
			return false;
		}
		if (innerToStream.getParentNode().isEmpty()
				|| !(innerToStream.getParentNode().get() instanceof MethodCallExpr)) {
			return false;
		}

		// The method over the innerFlapMap has to be moved as a method over the outerFlatMap
		MethodCallExpr overStreamCall = (MethodCallExpr) innerToStream.getParentNode().get();
		return doReplace(flatMapExpr, flatMapInnerMethodCall, innerToStream, overStreamCall, optFlatMapNextMethod);
	}

	private boolean doReplace(MethodCallExpr flatMapExpr,
			MethodCallExpr flatMapInnerMethodCall,
			MethodCallExpr innerToStream,
			MethodCallExpr overStreamCall,
			Optional<MethodCallExpr> optFlatMapNextMethod) {
		// replace `s.flatMap(row -> row.stream().filter(c -> c.isEmpty()))` into `stream.flatMap(r -> r.stream())`
		boolean replaced = tryReplace(flatMapInnerMethodCall, innerToStream);
		if (!replaced) {
			return false;
		}

		// replace `stream.flatMap(r -> r.stream())` into `stream.flatMap(r -> r.stream()).filter(c -> c.isEmpty())`
		optFlatMapNextMethod.ifPresent(next -> next.setScope(flatMapInnerMethodCall));

		// BEWARE this `.setScope` is done after the previous one , else JavaParsed LexicalPreservingPrinter fails
		overStreamCall.setScope(flatMapExpr);

		return true;
	}

	private Optional<MethodCallExpr> searchInnerToStream(MethodCallExpr flatMapInnerMethodCall) {
		MethodCallExpr innerToStream = flatMapInnerMethodCall;
		while (innerToStream.getScope().isPresent() && innerToStream.getScope().get() instanceof MethodCallExpr) {
			innerToStream = (MethodCallExpr) innerToStream.getScope().get();
		}

		if (innerToStream.equals(flatMapInnerMethodCall)) {
			// We encounter `.flatMap(s -> s.stream())`
			return Optional.empty();
		}
		return Optional.of(innerToStream);
	}
}
