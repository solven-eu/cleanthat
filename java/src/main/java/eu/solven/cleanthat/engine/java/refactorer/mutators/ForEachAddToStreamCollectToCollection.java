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
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ImportDeclarationHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ResolvedTypeHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * See {@link TestForEachAddToStreamCollectToCollectionCases}
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe({ LambdaIsMethodReference.class })
public class ForEachAddToStreamCollectToCollection extends ARefactorConsecutiveStatements {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream", "Loop");
	}

	@Override
	protected boolean trySimplifyingStatements(NodeAndSymbolSolver<BlockStmt> blockStmtAndSolver,
			Statement currentStmt,
			Statement nextStmt) {
		if (currentStmt.isExpressionStmt() && nextStmt.isForEachStmt()) {
			return onForEachStmt(blockStmtAndSolver, currentStmt.asExpressionStmt(), nextStmt.asForEachStmt());
		} else {
			return false;
		}
	}

	@SuppressWarnings({ "PMD.NPathComplexity", "PMD.CognitiveComplexity" })
	private boolean onForEachStmt(NodeAndSymbolSolver<BlockStmt> blockStmtAndSolver,
			ExpressionStmt asExpressionStmt,
			ForEachStmt forEachStmt) {
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

		if (!ResolvedTypeHelpers.typeIsAssignable(ResolvedTypeHelpers.optResolvedType(objectCreationExpr.getType()),
				Collection.class.getName()) || !objectCreationExpr.getArguments().isEmpty()) {
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

		var optStream = iterableToStream(blockStmtAndSolver, forEachStmt);
		if (optStream.isEmpty()) {
			return false;
		}
		var stream = optStream.get();

		MethodCallExpr streamMayDoFilter;
		if (optIfStmt.isPresent()) {
			var ifStmt = optIfStmt.get();
			Optional<LambdaExpr> filterArgument =
					ForEachIfToIfStreamAnyMatch.ifConditionToLambda(ifStmt, forEachStmt.getVariable().getVariable(0));
			if (filterArgument.isEmpty()) {
				return false;
			}

			streamMayDoFilter = new MethodCallExpr(stream, "filter", new NodeList<>(filterArgument.get()));
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

		// TODO Import `Collectors`
		var toCollection =
				new MethodCallExpr(new NameExpr("Collectors"), "toCollection", new NodeList<>(collectorLambda));

		variableDeclarationExpr.getVariable(0)
				.setInitializer(new MethodCallExpr(doMap, "collect", new NodeList<>(toCollection)));

		return true;
	}

	private Optional<MethodCallExpr> iterableToStream(NodeAndSymbolSolver<BlockStmt> blockStmtAndSolver,
			ForEachStmt forEachStmt) {
		// May be a Collection or an Array
		var iterable = forEachStmt.getIterable();

		if (MethodCallExprHelpers.scopeHasRequiredType(blockStmtAndSolver.editNode(iterable), Collection.class)) {
			return Optional.of(new MethodCallExpr(iterable, "stream"));
		} else {
			Optional<ResolvedDeclaration> optType = optResolved(iterable);

			if (MethodCallExprHelpers.scopeHasRequiredType(blockStmtAndSolver.editNode(iterable), int[].class)) {
				// TODO https://github.com/javaparser/javaparser/issues/3955
				// return Optional.of(new MethodCallExpr(iterable, "stream"));
				return Optional.empty();
			} else if (optType.isPresent() && optType.get().isParameter()
					&& optType.get().asParameter().getType().isArray()) {

				NameExpr scope = ImportDeclarationHelpers.nameOrQualifiedName(blockStmtAndSolver, Stream.class);
				return Optional.of(new MethodCallExpr(scope, "of", new NodeList<>(iterable)));
			} else {
				return Optional.empty();
			}
		}
	}
}
