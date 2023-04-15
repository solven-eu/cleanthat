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
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `for(User user : users) { if(!isImportantCustomer(user)) { break; } attachDiscount(user); }`
 * 
 * into `users.stream().takeWhile(user -> isImportantCustomer(user)).forEach(user -> applyDiscount(user));`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe({ LambdaIsMethodReference.class })
public class ForEachIfBreakElseToStreamTakeWhile extends AJavaparserStmtMutator {
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
		return Optional.of("EnhancedForLoopToStreamTakeWhile");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-take-while.html";
	}

	@Override
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isForEachStmt()) {
			return false;
		}

		var forEachStmt = stmt.asForEachStmt();

		if (!forEachStmt.getBody().isBlockStmt()) {
			return false;
		}
		BlockStmt forEachBlock = forEachStmt.getBody().asBlockStmt();
		if (forEachBlock.getStatements().size() <= 1) {
			// We expect an `if`, and other statements
			return false;
		}

		Optional<IfStmt> optIfStmt = StreamMutatorHelpers.findSingleIfThenStmt(forEachBlock.getStatements().get(0));
		if (optIfStmt.isEmpty()) {
			return false;
		}

		var ifStmt = optIfStmt.get();

		var filterArgument =
				ForEachIfToIfStreamAnyMatch.ifConditionToLambda(ifStmt, forEachStmt.getVariable().getVariable(0));
		if (filterArgument.isEmpty()) {
			return false;
		}

		BlockStmt streamForEachBlock = forEachBlock.clone();
		streamForEachBlock.getStatements().remove(0);

		Optional<LambdaExpr> lambdaExpr = LambdaExprHelpers
				.makeLambdaExpr(forEachStmt.getVariable().getVariable(0).getName(), streamForEachBlock);
		if (lambdaExpr.isEmpty()) {
			return false;
		} else {
			// BEWARE This seemingly fixes an issue in LexicalPreservingPrinter, as removal from the close seems not
			// enough
			forEachBlock.getStatements().remove(0);
		}

		MethodCallExpr stream = new MethodCallExpr(forEachStmt.getIterable(), "stream");
		MethodCallExpr takeWhile = new MethodCallExpr(stream, "takeWhile", new NodeList<>(filterArgument.get()));
		MethodCallExpr forEach = new MethodCallExpr(takeWhile, "forEach", new NodeList<>(lambdaExpr.get()));

		return tryReplace(forEachStmt, new ExpressionStmt(forEach));
	}

}
