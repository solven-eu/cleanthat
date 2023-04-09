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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyMeBefore;

/**
 * See EnhancedForLoopToStreamAnyMatchCases
 *
 * @author Benoit Lacelle
 */
@ApplyMeBefore({ SimplifyBooleanInitialization.class })
public class ForEachIfToIfStreamAnyMatch extends AJavaparserStmtMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ForEachIfToIfStreamAnyMatch.class);

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
		return Optional.of("EnhancedForLoopToStreamAnyMatch");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-any-match.html";
	}

	@Override
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isForEachStmt()) {
			return false;
		}

		var forEachStmt = stmt.asForEachStmt();

		Optional<IfStmt> optIfStmt = StreamMutatorHelpers.findSingleIfThenStmt(forEachStmt);
		if (optIfStmt.isEmpty()) {
			return false;
		}

		var ifStmt = optIfStmt.get();
		var thenStmt = ifStmt.getThenStmt();
		if (!thenStmt.isBlockStmt()) {
			return false;
		}

		var thenAsBlockStmt = thenStmt.asBlockStmt();
		if (thenAsBlockStmt.getStatements().isEmpty()) {
			// We expect a last statement to be either break or return
			return false;
		}

		if (hasOuterAssignExpr(ifStmt.getCondition())) {
			// We can not put a variableAssignement in a lambda
			return false;
		}

		var lastStmt = thenAsBlockStmt.getStatement(thenAsBlockStmt.getStatements().size() - 1);

		if (lastStmt.isReturnStmt()) {
			NameExpr variableName = forEachStmt.getVariableDeclarator().getNameAsExpression();
			Optional<NameExpr> optVariableIsReturned =
					thenAsBlockStmt.findFirst(NameExpr.class, n -> variableName.equals(n));

			if (optVariableIsReturned.isPresent()) {
				// The return statement depends on the variable
				// TODO EnhancedForLoopToStreamAnyMatchCases.ReturnVariable
				return false;
			} else {
				// The return statement does not depend on the variable
				return replaceForEachIfByIfStream(forEachStmt, ifStmt, thenAsBlockStmt);
			}
		} else if (lastStmt.isBreakStmt()) {
			boolean breakIsRemoved = tryRemove(lastStmt);
			if (!breakIsRemoved) {
				return false;
			}

			var replaced = replaceForEachIfByIfStream(forEachStmt, ifStmt, thenAsBlockStmt);

			if (!replaced) {
				// We restore the `break`
				LOGGER.debug("We try restoring the `break` as we failed replacing the `forEach(if)`");
				// BEWARE it would have been better not to remove it
				if (!thenAsBlockStmt.getStatements().add(lastStmt)) {
					throw new IllegalStateException(
							"We corrupted `" + stmt + "` by failing restoring `" + lastStmt + "`");
				}
			}

			return replaced;
		} else {
			return false;
		}
	}

	protected boolean replaceForEachIfByIfStream(ForEachStmt forEachStmt, IfStmt ifStmt, BlockStmt thenAsBlockStmt) {
		Expression withStream = new MethodCallExpr(forEachStmt.getIterable(), "stream");
		var variable = forEachStmt.getVariable().getVariables().get(0);
		var lambdaExpr = ifConditionToLambda(ifStmt, variable);
		Expression withStream2 = new MethodCallExpr(withStream, ANY_MATCH, new NodeList<>(lambdaExpr));

		var newif = new IfStmt(withStream2, thenAsBlockStmt, null);

		return tryReplace(forEachStmt, newif);
	}

	public static LambdaExpr ifConditionToLambda(IfStmt ifStmt, VariableDeclarator variable) {
		// No need for variable.getType()
		var parameter = new Parameter(new UnknownType(), variable.getName());

		var condition = ifStmt.getCondition();

		var lambdaExpr = new LambdaExpr(parameter, condition);
		return lambdaExpr;
	}
}
