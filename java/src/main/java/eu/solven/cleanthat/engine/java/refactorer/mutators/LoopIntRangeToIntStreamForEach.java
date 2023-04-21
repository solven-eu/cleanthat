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
import java.util.stream.IntStream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ImportDeclarationHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `for (int i = 0 ; i < 10 ; i ++) {System.out.println(string);}`
 * 
 * into `IntStream.range(0, 10).forEach(j -> System.out.println(j));`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe({
		// As this create a LambdaExpr, the LambdaExpr may be optimizable
		LambdaReturnsSingleStatement.class,
		// As this create an IntStream, it may be optimizable
		StreamWrappedVariableToMap.class })
public class LoopIntRangeToIntStreamForEach extends AJavaparserStmtMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Primitive", "Loop", "Stream");
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processStatement(NodeAndSymbolSolver<Statement> stmt) {
		if (!stmt.getNode().isForStmt()) {
			return false;
		}

		var forStmt = stmt.getNode().asForStmt();

		if (forStmt.getInitialization().size() != 1) {
			// Stick to simple loops
			return false;
		} else if (!forStmt.getInitialization().get(0).isVariableDeclarationExpr()) {
			// TODO Handle assignExpr
			return false;
		}

		VariableDeclarationExpr singleDeclaration = forStmt.getInitialization().get(0).asVariableDeclarationExpr();
		if (singleDeclaration.getVariables().size() != 1) {
			return false;
		}

		VariableDeclarator singleVariable = singleDeclaration.getVariables().get(0);

		if (singleVariable.getInitializer().isEmpty()) {
			return false;
		} else if (!MethodCallExprHelpers.scopeHasRequiredType(stmt.editNode(singleDeclaration), int.class)) {
			return false;
		}

		if (forStmt.getCompare().isEmpty() || !forStmt.getCompare().get().isBinaryExpr()) {
			return false;
		}
		BinaryExpr compareBinaryExpr = forStmt.getCompare().get().asBinaryExpr();
		NameExpr iteratedVariableName = new NameExpr(singleVariable.getName());
		if (compareBinaryExpr.getOperator() != BinaryExpr.Operator.LESS
				|| !compareBinaryExpr.getLeft().equals(iteratedVariableName)) {
			return false;
		}

		// We look for a simple `i++` or `++i` or `i += 1`
		{
			if (forStmt.getUpdate().size() != 1) {
				return false;
			}

			if (forStmt.getUpdate().get(0).isUnaryExpr()) {
				UnaryExpr unaryUpdate = forStmt.getUpdate().get(0).asUnaryExpr();
				if (unaryUpdate.getOperator() != UnaryExpr.Operator.POSTFIX_INCREMENT
						&& unaryUpdate.getOperator() != UnaryExpr.Operator.PREFIX_INCREMENT) {
					return false;
				}
			} else if (forStmt.getUpdate().get(0).isAssignExpr()) {
				AssignExpr assignExprUpdate = forStmt.getUpdate().get(0).asAssignExpr();
				if (assignExprUpdate.getOperator() != AssignExpr.Operator.PLUS) {
					return false;
				} else if (assignExprUpdate.getTarget().equals(iteratedVariableName)) {
					return false;
				} else if (new IntegerLiteralExpr("1").equals(assignExprUpdate.getValue())) {
					return false;
				}
			} else {
				return false;
			}
		}

		Statement body = forStmt.getBody();
		Optional<LambdaExpr> lambdaExpr = LambdaExprHelpers.makeLambdaExpr(singleVariable.getName(), body);
		if (lambdaExpr.isEmpty()) {
			return false;
		}

		Expression from = singleVariable.getInitializer().get();
		Expression to = compareBinaryExpr.getRight();

		MethodCallExpr intRange =
				new MethodCallExpr(ImportDeclarationHelpers.nameOrQualifiedName(stmt, IntStream.class),
						"range",
						new NodeList<>(from, to));

		MethodCallExpr forEach = new MethodCallExpr(intRange, "forEach", new NodeList<>(lambdaExpr.get()));
		return tryReplace(forStmt, new ExpressionStmt(forEach));
	}
}
