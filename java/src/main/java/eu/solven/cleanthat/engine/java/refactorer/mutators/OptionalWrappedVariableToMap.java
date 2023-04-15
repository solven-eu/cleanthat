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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
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
public class OptionalWrappedVariableToMap extends AJavaparserExprMutator implements IReApplyUntilNoop {

	private static final String METHOD_MAP = "map";

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Optional", "Primitive");
	}

	protected Set<String> getEligibleForUnwrappedMap() {
		return Set.of("ifPresent", "ifPresentOrElse", METHOD_MAP);
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
		if (!getEligibleForUnwrappedMap().contains(methodCallExpr.getNameAsString())) {
			return false;
		} else if (!scopeHasRequiredType(methodCallExpr.getScope(), getExpectedScope())) {
			return false;
		} else if (methodCallExpr.getArguments().size() < 1) {
			return false;
		}

		Expression optLambdaExpr = methodCallExpr.getArguments().get(0);
		if (!optLambdaExpr.isLambdaExpr()) {
			return false;
		}

		LambdaExpr mapLambdaExpr = optLambdaExpr.asLambdaExpr();

		if (!mapLambdaExpr.getBody().isBlockStmt()) {
			// e.g. A MethodRefExpr can not be split
			return false;
		} else if (mapLambdaExpr.getParameters().size() != 1) {
			return false;
		}

		BlockStmt lambdaBlock = mapLambdaExpr.getBody().asBlockStmt();
		if (lambdaBlock.getStatements().size() <= 1) {
			// We expect at least a variableDeclaration, and something processing the new variable
			return false;
		}

		Statement firstStatement = lambdaBlock.getStatement(0);
		if (!firstStatement.isExpressionStmt()
				|| !firstStatement.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
			// We expect the first expression to be a variableDeclaration
			return false;
		}

		VariableDeclarationExpr variableDeclaratorExpr =
				firstStatement.asExpressionStmt().getExpression().asVariableDeclarationExpr();
		if (variableDeclaratorExpr.getVariables().size() != 1
				|| variableDeclaratorExpr.getVariable(0).getInitializer().isEmpty()) {
			// Leave if not a simple+single declaration
			return false;
		}

		VariableDeclarator firstVariable = variableDeclaratorExpr.getVariable(0);
		if (firstVariable.getInitializer().get().isNameExpr()) {
			// It is not relevant to introduce a `.map(l -> l)`
			return false;
		}

		Expression mapScope = methodCallExpr.getScope().get();
		Optional<String> optMapMethodName = computeMapMethodName(mapScope, variableDeclaratorExpr.getElementType());
		if (optMapMethodName.isEmpty()) {
			return false;
		}

		SimpleName lambdaVariableName = mapLambdaExpr.getParameters().get(0).getName();
		String lambdaVariableNameAsString = lambdaVariableName.asString();
		if (variableDeclaratorExpr
				.findFirst(NameExpr.class, nameExpr -> nameExpr.getNameAsString().equals(lambdaVariableNameAsString))
				.isEmpty()) {
			// We expect the variableDeclaration to be based on the lambda input
			return false;
		} else if (!lambdaBlock
				.findAll(NameExpr.class,
						nameExpr -> nameExpr.getNameAsString().equals(lambdaVariableNameAsString)
								&& !isAncestor(variableDeclaratorExpr, nameExpr))
				.isEmpty()) {
			// We ensure the only lambda input usage is for the initial variable declaration
			return false;
		}

		if (!tryRemove(firstStatement)) {
			// Remove the variableDeclaration as it is moved to the `.map(...)` methodCall
			return false;
		}
		if (!LambdaExprHelpers.changeName(mapLambdaExpr, variableDeclaratorExpr.getVariable(0).getName())) {
			return false;
		}

		Optional<LambdaExpr> unwrappedMapLambdaExpr = LambdaExprHelpers.makeLambdaExpr(lambdaVariableName,
				variableDeclaratorExpr.getVariable(0).getInitializer().get());
		if (unwrappedMapLambdaExpr.isEmpty()) {
			return false;
		}

		MethodCallExpr callMap =
				new MethodCallExpr(mapScope, optMapMethodName.get(), new NodeList<>(unwrappedMapLambdaExpr.get()));

		methodCallExpr.setScope(callMap);

		// We restore the parent, as previous `setScope` removed the parent
		// This looks like a bug/missing_feature to report to javaParser
		mapScope.setParentNode(callMap);

		adjustMethodName(methodCallExpr);

		return true;
	}

	/**
	 * 
	 * @param ancestor
	 * @param descendant
	 * @return true if given ancestor is an ancestor of given descendant
	 */
	private boolean isAncestor(Node ancestor, Node descendant) {
		return descendant.findAncestor(n -> n == ancestor, Node.class).isPresent();
	}

	protected Optional<String> computeMapMethodName(Expression expression, Type type) {
		return Optional.of(METHOD_MAP);
	}

	protected void adjustMethodName(MethodCallExpr methodCallExpr) {
		// No need to change the methodName by default (Optional)
	}
}
