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
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserStmtMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `for (String string : stringList) {System.out.println(string);}`
 * 
 * into `stringList.forEach(s -> System.out.println(s));`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
public class EnhancedForLoopToForEach extends AJavaparserStmtMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Iterable", "Loop");
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
	protected boolean processNotRecursively(Statement stmt) {
		if (!stmt.isForEachStmt()) {
			return false;
		}

		var forEachStmt = stmt.asForEachStmt();

		if (!canBePushedInLambdaExpr(forEachStmt.getBody())) {
			// We can not move AssignExpr inside a LambdaExpr
			return false;
		}

		Optional<ResolvedDeclaration> resolved = optResolved(forEachStmt.getIterable());
		if (resolved.isEmpty()) {
			// We need to make sure the type is not an array
			return false;
		} else if (!(resolved.get() instanceof ResolvedValueDeclaration)) {
			return false;
		}
		ResolvedValueDeclaration resolvedValueDeclaration = (ResolvedValueDeclaration) resolved.get();
		if (resolvedValueDeclaration.getType().isArray()) {
			// TODO Handle this case
			return false;
		}

		LambdaExpr lambdaExpr;
		var parameter = new Parameter(new UnknownType(), forEachStmt.getVariableDeclarator().getName());
		if (forEachStmt.getBody().isBlockStmt()) {
			lambdaExpr = new LambdaExpr(parameter, forEachStmt.getBody().asBlockStmt());
		} else if (forEachStmt.getBody().isExpressionStmt()) {
			lambdaExpr = new LambdaExpr(parameter, forEachStmt.getBody().asExpressionStmt().getExpression());
		} else {
			return false;
		}

		MethodCallExpr forEach = new MethodCallExpr(forEachStmt.getIterable(), "forEach", new NodeList<>(lambdaExpr));
		return tryReplace(forEachStmt, new ExpressionStmt(forEach));
	}
}
