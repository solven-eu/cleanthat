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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.helpers.LambdaExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;
import eu.solven.cleanthat.engine.java.refactorer.meta.IReApplyUntilNoop;

/**
 * Turns `o.ifPresent(s -> { String subString = s.substring(1); System.out.println(subString); });`
 * 
 * into `o.map(s -> s.substring(1)).ifPresent(subString -> { System.out.println(subString); });`
 *
 * @author Benoit Lacelle
 */
// https://stackoverflow.com/questions/29104968/can-i-not-map-flatmap-an-optionalint
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
public class OptionalWrappedIfToFilter extends AJavaparserExprMutator implements IReApplyUntilNoop {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Optional");
	}

	protected Set<String> getEligibleForUnwrappedFilter() {
		return Set.of("ifPresent", "ifPresentOrElse");
	}

	protected Class<?> getExpectedScope() {
		return Optional.class;
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}

		MethodCallExpr methodCallExpr = expr.asMethodCallExpr();
		if (!getEligibleForUnwrappedFilter().contains(methodCallExpr.getNameAsString())) {
			return false;
		} else if (!scopeHasRequiredType(methodCallExpr.getScope(), getExpectedScope())) {
			return false;
		} else if (methodCallExpr.getArguments().size() < 1) {
			return false;
		}

		Expression mayLambdaExpr = methodCallExpr.getArguments().get(0);
		if (!mayLambdaExpr.isLambdaExpr()) {
			return false;
		}

		LambdaExpr ifPresentLambdaExpr = mayLambdaExpr.asLambdaExpr();

		if (!ifPresentLambdaExpr.getBody().isBlockStmt()) {
			// e.g. A MethodRefExpr can not be split
			return false;
		} else if (ifPresentLambdaExpr.getParameters().size() != 1) {
			return false;
		}

		BlockStmt lambdaBlock = ifPresentLambdaExpr.getBody().asBlockStmt();
		if (lambdaBlock.getStatements().size() != 1) {
			// We expect an IfStmt as single statement
			return false;
		}

		Statement firstStatement = lambdaBlock.getStatement(0);
		if (!firstStatement.isIfStmt()) {
			// We expect the first expression to be a IfStmt
			return false;
		}

		IfStmt ifStmt = firstStatement.asIfStmt();
		if (ifStmt.hasElseBranch()) {
			// We can not unwrapped in a filter if there is an `else`
			return false;
		}

		Statement thenStmt = ifStmt.getThenStmt();
		Optional<LambdaExpr> optNewLambdaExpr =
				LambdaExprHelpers.makeLambdaExpr(ifPresentLambdaExpr.getParameter(0).getName(), thenStmt);
		if (optNewLambdaExpr.isEmpty()) {
			return false;
		}

		Expression condition = ifStmt.getCondition();
		if (condition
				.findFirst(NameExpr.class,
						n -> n.getNameAsString().equals(ifPresentLambdaExpr.getParameter(0).getNameAsString()))
				.isEmpty()) {
			// The condition is not dependent of the lambdaExpr variableName
			return false;
		}

		Optional<LambdaExpr> optFilterLambdaExpr = LambdaExprHelpers
				.makeLambdaExpr(ifPresentLambdaExpr.getParameter(0).getName(), new ExpressionStmt(condition));
		if (optFilterLambdaExpr.isEmpty()) {
			return false;
		}

		tryReplace(ifPresentLambdaExpr, optNewLambdaExpr.get());

		tryReplace(ifStmt, thenStmt);

		Expression filterScope = methodCallExpr.getScope().get();
		MethodCallExpr callFilter =
				new MethodCallExpr(filterScope, "filter", new NodeList<>(optFilterLambdaExpr.get()));
		methodCallExpr.setScope(callFilter);

		// We restore the parent, as previous `setScope` removed the parent
		// This looks like a bug/missing_feature to report to javaParser
		filterScope.setParentNode(callFilter);

		return true;
	}
}
