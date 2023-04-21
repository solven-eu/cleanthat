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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ImportDeclarationHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ResolvedTypeHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.VariableDeclarationExprHepers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `String key = ""; for (String value : values) { if (value.length() > 4) { key = value; break; } }`
 * 
 * into `String key = values.stream().filter(value -> value.length() > 4).findFirst().orElse("");`
 *
 * @author Benoit Lacelle
 */
// This is similar to SimplifyBooleanInitialization
@ApplyAfterMe({ LambdaIsMethodReference.class })
public class ForEachIfBreakToStreamFindFirst extends ARefactorConsecutiveStatements {
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream", "Loop", "Initialization");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("EnhancedForLoopToStreamFindFirst");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-find-first.html";
	}

	@SuppressWarnings({ "PMD.NPathComplexity", "PMD.CognitiveComplexity" })
	@Override
	protected boolean trySimplifyingStatements(NodeAndSymbolSolver<BlockStmt> blockStmtAndSolver,
			Statement currentStmt,
			Statement nextStmt) {
		Optional<VariableDeclarationExpr> optAssignExpr =
				VariableDeclarationExprHepers.optSimpleDeclaration(currentStmt);
		if (optAssignExpr.isEmpty()) {
			return false;
		}
		var assignExpr = optAssignExpr.get();

		var singleVariable = assignExpr.getVariable(0);
		Optional<Expression> optInitializer = singleVariable.getInitializer();
		if (optInitializer.isEmpty()) {
			return false;
		}

		if (!nextStmt.isForEachStmt()) {
			return false;
		}
		var forEachStmt = nextStmt.asForEachStmt();
		if (!forEachStmt.getBody().isBlockStmt() || forEachStmt.getBody().asBlockStmt().getStatements().size() != 1
				|| !forEachStmt.getBody().asBlockStmt().getStatement(0).isIfStmt()
				// We need blockStmt as we expect a variableAssignement and a breakStmt
				|| !forEachStmt.getBody().asBlockStmt().getStatement(0).asIfStmt().hasThenBlock()) {
			return false;
		}

		IfStmt ifStmt = forEachStmt.getBody().asBlockStmt().getStatement(0).asIfStmt();
		BlockStmt thenStmt = ifStmt.getThenStmt().asBlockStmt();
		if (thenStmt.getStatements().size() != 2 || !thenStmt.getStatement(1).isBreakStmt()) {
			// We looks for an assignExpr, then a breakStmt
			return false;
		}

		Optional<AssignExpr> optIfAssignExpr =
				SimplifyBooleanInitialization.searchSingleAssignExpr(thenStmt.getStatement(0));
		if (optIfAssignExpr.isEmpty()) {
			return false;
		}

		var ifAssignExpr = optIfAssignExpr.get();
		if (SimplifyBooleanInitialization.notAssignOperator(ifAssignExpr) || !ifAssignExpr.getTarget().isNameExpr()
				|| !ifAssignExpr.getTarget().asNameExpr().getNameAsString().equals(singleVariable.getNameAsString())) {
			return false;
		}

		MethodCallExpr streamCall = new MethodCallExpr(forEachStmt.getIterable(), "stream");
		VariableDeclarator forEachVariable = forEachStmt.getVariableDeclarator();
		MethodCallExpr filterCall;
		{
			Optional<LambdaExpr> lambdaExpr =
					LambdaExprHelpers.makeLambdaExpr(forEachVariable.getName(), ifStmt.getCondition());
			if (lambdaExpr.isEmpty()) {
				return false;
			}
			filterCall = new MethodCallExpr(streamCall, "filter", new NodeList<>(lambdaExpr.get()));
		}
		MethodCallExpr findFirstCall = new MethodCallExpr(filterCall, "findFirst");

		if (!ifAssignExpr.getValue().isNameExpr()) {
			Optional<LambdaExpr> optLambdaExpr =
					LambdaExprHelpers.makeLambdaExpr(forEachVariable.getName(), ifAssignExpr.getValue());
			if (optLambdaExpr.isEmpty()) {
				return false;
			}
			findFirstCall = new MethodCallExpr(findFirstCall, "map", new NodeList<>(optLambdaExpr.get()));
		}

		Optional<ResolvedType> optVariableType = ResolvedTypeHelpers.optResolvedType(singleVariable.getType());
		Optional<ResolvedType> optForEachType = ResolvedTypeHelpers.optResolvedType(forEachVariable.getType());
		if (optVariableType.isEmpty() || optForEachType.isEmpty()) {
			return false;
		}

		{
			ResolvedType variableResolvedType = optVariableType.get();
			ResolvedType forEachType = optForEachType.get();

			// !ResolvedTypeHelpers.areSameType(forEachType, variableResolvedType)
			if (!forEachType.isAssignableBy(variableResolvedType)) {
				MethodReferenceExpr methodReferenceExpr;
				if (variableResolvedType.isPrimitive()) {
					// Primitive are managed specifically to prevent unnecessary casting
					ResolvedPrimitiveType asPrimitive = variableResolvedType.asPrimitive();

					// There is an implicit cast between 2 primitive types
					methodReferenceExpr =
							new MethodReferenceExpr(ImportDeclarationHelpers.nameOrQualifiedName(blockStmtAndSolver,
									asPrimitive.getBoxTypeClass()), new NodeList<>(), "valueOf");
				} else {

					// There is an implicit cast
					String typeName = assignExpr.getElementType().asString();
					methodReferenceExpr = new MethodReferenceExpr(new FieldAccessExpr(new NameExpr(typeName), "class"),
							new NodeList<>(),
							"cast");
				}
				findFirstCall = new MethodCallExpr(findFirstCall, "map", new NodeList<>(methodReferenceExpr));
			}
		}

		var defaultExpr = optInitializer.get();
		MethodCallExpr orElseCall = new MethodCallExpr(findFirstCall, "orElse", new NodeList<>(defaultExpr));

		singleVariable.setInitializer(orElseCall);

		tryRemove(forEachStmt);

		return true;
	}

}
