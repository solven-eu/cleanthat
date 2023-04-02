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
import com.github.javaparser.ast.body.Parameter;
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
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `for (int i = 0 ; i < 10 ; i ++) {System.out.println(string);}`
 * 
 * into `IntStream.range(0, 10).forEach(j -> System.out.println(j));`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
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
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isForStmt()) {
			return false;
		}

		var forStmt = stmt.asForStmt();

		Statement body = forStmt.getBody();
		{
			if (hasOuterAssignExpr(body)) {
				// We can not move AssignExpr inside a LambdaExpr
				return false;
			} else if (body.findFirst(ReturnStmt.class).isPresent()) {
				// Can can not move the `return` inside a LambdaExpr
				return false;
			} else if (body.findFirst(ContinueStmt.class).isPresent()) {
				// TODO We would need to turn `continue;` into `return;`
				// BEWARE of `continue: outerLoop;`
				return false;
			} else if (body.findFirst(BreakStmt.class).isPresent()) {
				return false;
			}
		}

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
		} else if (!scopeHasRequiredType(Optional.of(singleDeclaration), int.class)) {
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

		LambdaExpr lambdaExpr;
		var parameter = new Parameter(new UnknownType(), singleVariable.getName());
		if (body.isBlockStmt()) {
			lambdaExpr = new LambdaExpr(parameter, body.asBlockStmt());
		} else if (body.isExpressionStmt()) {
			lambdaExpr = new LambdaExpr(parameter, body.asExpressionStmt().getExpression());
		} else {
			return false;
		}

		Expression from = singleVariable.getInitializer().get();
		Expression to = compareBinaryExpr.getRight();

		MethodCallExpr intRange = new MethodCallExpr(new NameExpr("IntStream"), "range", new NodeList<>(from, to));

		MethodCallExpr forEach = new MethodCallExpr(intRange, "forEach", new NodeList<>(lambdaExpr));
		return tryReplace(forStmt, new ExpressionStmt(forEach));
	}
}
