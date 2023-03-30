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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;

/**
 * See {@link EnhancedForLoopToStreamCollectCases}
 *
 * @author Benoit Lacelle
 */
public class EnhancedForLoopToStreamCollect extends AJavaparserStmtMutator {

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream", "Loop");
	}

	@Override
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isBlockStmt()) {
			return false;
		}

		var blockStmt = stmt.asBlockStmt();

		if (blockStmt.getStatements().size() <= 1) {
			return false;
		}

		var modified = false;

		for (var i = 0; i < blockStmt.getStatements().size() - 1; i++) {
			var first = blockStmt.getStatement(i);
			var second = blockStmt.getStatement(i + 1);

			if (first.isExpressionStmt() && second.isForEachStmt()) {
				modified |= onForEachStmt(first.asExpressionStmt(), second.asForEachStmt());
			}
		}

		return modified;
	}

	@SuppressWarnings({ "PMD.NPathComplexity", "PMD.CognitiveComplexity" })
	private boolean onForEachStmt(ExpressionStmt asExpressionStmt, ForEachStmt forEachStmt) {
		if (!asExpressionStmt.getExpression().isVariableDeclarationExpr()) {
			return false;
		}
		var variableDeclarationExpr = asExpressionStmt.getExpression().asVariableDeclarationExpr();
		if (variableDeclarationExpr.getVariables().size() != 1) {
			return false;
		}
		Optional<Expression> optInitializer = variableDeclarationExpr.getVariable(0).getInitializer();
		if (optInitializer.isEmpty() || !optInitializer.get().isObjectCreationExpr()) {
			return false;
		}
		var objectCreationExpr = optInitializer.get().asObjectCreationExpr();

		if (!typeHasRequiredType(optResolvedType(objectCreationExpr.getType()), Collection.class.getName())
				|| !objectCreationExpr.getArguments().isEmpty()) {
			return false;
		}

		Statement thenStmt;
		Optional<IfStmt> optIfStmt = StreamMutatorHelpers.findSingleIfThenStmt(forEachStmt);
		if (optIfStmt.isPresent()) {
			thenStmt = optIfStmt.get().getThenStmt();
		} else {
			// Do this before, as it is called by `StreamMutatorHelpers.findSingleIfThenStmt`
			Optional<Statement> optSingleForEachBodyStmt =
					StreamMutatorHelpers.findSingleStatement(forEachStmt.getBody());
			if (optSingleForEachBodyStmt.isEmpty()) {
				return false;
			}
			thenStmt = optSingleForEachBodyStmt.get();
		}

		Optional<Statement> optMethodCall = StreamMutatorHelpers.findSingleStatement(thenStmt);
		if (optMethodCall.isEmpty()) {
			return false;
		}

		if (!optMethodCall.get().isExpressionStmt()) {
			return false;
		}
		if (!optMethodCall.get().asExpressionStmt().getExpression().isMethodCallExpr()) {
			return false;
		}
		var methodCall = optMethodCall.get().asExpressionStmt().getExpression().asMethodCallExpr();
		if (!"add".equals(methodCall.getNameAsString()) && !"addAll".equals(methodCall.getNameAsString())) {
			return false;
		} else if (methodCall.getScope().isEmpty() || !methodCall.getScope().get().isNameExpr()) {
			return false;
		} else if (!Objects.equals(methodCall.getScope().get().asNameExpr(),
				variableDeclarationExpr.getVariable(0).getNameAsExpression())) {
			return false;
		}

		if (methodCall.getArguments().size() != 1) {
			return false;
		}

		var optStream = iterableToStream(forEachStmt);
		if (optStream.isEmpty()) {
			return false;
		}
		var stream = optStream.get();

		MethodCallExpr streamMayDoFilter;
		if (optIfStmt.isPresent()) {
			var ifStmt = optIfStmt.get();
			LambdaExpr filterArgument = EnhancedForLoopToStreamAnyMatch.ifConditionToLambda(ifStmt,
					forEachStmt.getVariable().getVariable(0));
			streamMayDoFilter = new MethodCallExpr(stream, "filter", new NodeList<>(filterArgument));
		} else {
			streamMayDoFilter = stream;
		}

		// Make sure this is removed after any necessary type resolution
		boolean removedForEach = tryRemove(forEachStmt);
		if (!removedForEach) {
			return false;
		}

		var added = methodCall.getArgument(0);
		String maporFlatMap;
		if ("add".equals(methodCall.getNameAsString())) {
			maporFlatMap = "map";
		} else if ("addAll".equals(methodCall.getNameAsString())) {
			maporFlatMap = "flatMap";

			// `.addAll` accepts a Collection, while `.flatMap` consumes a Stream
			added = new MethodCallExpr(added, "stream");
		} else {
			return false;
		}

		var parameter = new Parameter(new UnknownType(), forEachStmt.getVariableDeclarator().getName());
		var doMap =
				new MethodCallExpr(streamMayDoFilter, maporFlatMap, new NodeList<>(new LambdaExpr(parameter, added)));

		var collectorLambda = new LambdaExpr(new NodeList<>(), objectCreationExpr);

		var toCollection =
				new MethodCallExpr(new NameExpr("Collectors"), "toCollection", new NodeList<>(collectorLambda));

		variableDeclarationExpr.getVariable(0)
				.setInitializer(new MethodCallExpr(doMap, "collect", new NodeList<>(toCollection)));

		return true;
	}

	private Optional<MethodCallExpr> iterableToStream(ForEachStmt forEachStmt) {
		// May be a Collection or an Array
		var iterable = forEachStmt.getIterable();

		if (scopeHasRequiredType(Optional.of(iterable), Collection.class)) {
			return Optional.of(new MethodCallExpr(iterable, "stream"));
		} else {
			Optional<ResolvedDeclaration> optType = optResolved(iterable);

			if (scopeHasRequiredType(Optional.of(iterable), int[].class)) {
				// TODO https://github.com/javaparser/javaparser/issues/3955
				// return Optional.of(new MethodCallExpr(iterable, "stream"));
				return Optional.empty();
			} else if (optType.isPresent() && optType.get().isParameter()
					&& optType.get().asParameter().getType().isArray()) {

				return Optional.of(new MethodCallExpr(
						new NameExpr(nameOrQualifiedName(forEachStmt.findCompilationUnit().get(), Stream.class)),
						"of",
						new NodeList<>(iterable)));
			} else {
				return Optional.empty();
			}
		}
	}
}
