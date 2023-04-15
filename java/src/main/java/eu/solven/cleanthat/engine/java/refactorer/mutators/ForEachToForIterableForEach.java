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
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `for (String string : stringList) {System.out.println(string);}`
 * 
 * into `stringList.forEach(s -> System.out.println(s));`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
public class ForEachToForIterableForEach extends AJavaparserStmtMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Iterable", "Loop", "Stream");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("EnhancedForLoopToStreamForEach");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/enhanced-for-loop-to-stream-for-each.html";
	}

	@Override
	public Set<String> getLegacyIds() {
		return Set.of("EnhancedForLoopToForEach");
	}

	@Override
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isForEachStmt()) {
			return false;
		}

		var forEachStmt = stmt.asForEachStmt();

		Optional<ResolvedType> resolved = optResolvedType(forEachStmt.getIterable());
		if (resolved.isEmpty()) {
			// We need to make sure the type is not an array
			return false;
		} else if (resolved.get().isArray()) {
			// TODO Handle iteration over arrays
			return false;
		}

		Optional<LambdaExpr> optLambdaExpr =
				LambdaExprHelpers.makeLambdaExpr(forEachStmt.getVariableDeclarator().getName(), forEachStmt.getBody());
		if (optLambdaExpr.isEmpty()) {
			return false;
		}

		MethodCallExpr forEach =
				new MethodCallExpr(forEachStmt.getIterable(), "forEach", new NodeList<>(optLambdaExpr.get()));
		return tryReplace(forEachStmt, new ExpressionStmt(forEach));
	}
}
