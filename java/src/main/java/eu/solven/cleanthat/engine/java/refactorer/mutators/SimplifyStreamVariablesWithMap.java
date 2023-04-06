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
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.PrimitiveType.Primitive;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyAfterMe;

/**
 * Turns `IntStream.range(0, s.length()).forEach(j -> { int c = s.charAt(j); sb.append(c); });`
 * 
 * into `IntStream.range(0, s.length()).map(j -> s.charAt(j)).forEach(c -> sb.append(c))`
 *
 * @author Benoit Lacelle
 */
@ApplyAfterMe(LambdaReturnsSingleStatement.class)
public class SimplifyStreamVariablesWithMap extends AJavaparserExprMutator {

	private static final String METHOD_MAP = "map";
	private static final String METHOD_MAP_TO_INT = "mapToInt";
	private static final String METHOD_MAP_TO_LNG = "mapToLong";
	private static final String METHOD_MAP_TO_DBL = "mapToDouble";

	private static final Set<String> ELIGIBLE_FOR_INTERMEDIATE_MAP =
			Set.of("forEach", METHOD_MAP, METHOD_MAP_TO_INT, METHOD_MAP_TO_LNG, METHOD_MAP_TO_DBL);

	// These methods are turned into a p;lain `.map` by adding a prefix `.map[XXX]`
	private static final Set<String> SIMPLIFIED_TO_MAP =
			Set.of(METHOD_MAP_TO_INT, METHOD_MAP_TO_LNG, METHOD_MAP_TO_DBL);

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
	protected boolean processNotRecursively(Expression expr) {
		if (!expr.isMethodCallExpr()) {
			return false;
		}

		MethodCallExpr methodCallExpr = expr.asMethodCallExpr();
		if (!ELIGIBLE_FOR_INTERMEDIATE_MAP.contains(methodCallExpr.getNameAsString())) {
			return false;
		} else if (!scopeHasRequiredType(methodCallExpr.getScope(), BaseStream.class)) {
			return false;
		} else if (methodCallExpr.getArguments().size() != 1) {
			return false;
		}

		Expression optLambdaExpr = methodCallExpr.getArguments().get(0);
		if (!optLambdaExpr.isLambdaExpr()) {
			return false;
		}

		LambdaExpr lambdaExpr = optLambdaExpr.asLambdaExpr();

		if (!lambdaExpr.getBody().isBlockStmt()) {
			// e.g. A MethodRefExpr can not be split
			return false;
		} else if (lambdaExpr.getParameters().size() != 1) {
			return false;
		}

		BlockStmt lambdaBlock = lambdaExpr.getBody().asBlockStmt();
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

		Optional<String> optMapMethodName =
				computeMapMethodName(methodCallExpr.getScope().get(), variableDeclaratorExpr.getElementType());
		if (optMapMethodName.isEmpty()) {
			return false;
		}

		String lambdaVariableName = lambdaExpr.getParameters().get(0).getNameAsString();
		if (variableDeclaratorExpr
				.findFirst(NameExpr.class, nameExpr -> nameExpr.getNameAsString().equals(lambdaVariableName))
				.isEmpty()) {
			// We expect the variableDeclaration to be based on the lambda input
			return false;
		} else if (!lambdaBlock
				.findAll(NameExpr.class,
						nameExpr -> nameExpr.getNameAsString().equals(lambdaVariableName)
								&& !isAncestor(variableDeclaratorExpr, nameExpr))
				.isEmpty()) {
			// We ensure the only lambda input usage is for the initial variable declaration
			return false;
		}

		// 1- The following makes a typo replacing the variableName
		// lambdaExpr.getParameter(0).setName(variable.getVariable(0).getName());
		// 2- The following may lead to exception in LexicalPreservingPrinter
		var newForEachVariable = new Parameter(new UnknownType(), variableDeclaratorExpr.getVariable(0).getName());
		// lambdaExpr.setParameter(0, newForEachVariable);
		// LambdaExpr cloneLambdaExpr = lambdaExpr.clone();
		// methodCallExpr.setArgument(0, cloneLambdaExpr);
		// lambdaExpr.getParameter(0).setName(variable.getVariable(0).getName());
		// https://github.com/javaparser/javaparser/issues/3898#issuecomment-1426961297
		// lambdaExpr.setEnclosingParameters(false);
		// lambdaExpr.getParameter(0).replace(newForEachVariable);

		lambdaExpr.replace(new LambdaExpr(newForEachVariable, lambdaBlock));

		if (!tryRemove(firstStatement)) {
			// Remove the variableDeclaration as it is moved to the `.map(...)` methodCall
			return false;
		}

		var parameter = new Parameter(new UnknownType(), lambdaVariableName);
		LambdaExpr mapLambdaExpr =
				new LambdaExpr(parameter, variableDeclaratorExpr.getVariable(0).getInitializer().get());

		MethodCallExpr callMap = new MethodCallExpr(methodCallExpr.getScope().get(),
				optMapMethodName.get(),
				new NodeList<>(mapLambdaExpr));

		methodCallExpr.setScope(callMap);
		if (SIMPLIFIED_TO_MAP.contains(methodCallExpr.getNameAsString())) {
			methodCallExpr.setName(METHOD_MAP);
		}

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

	private Optional<String> computeMapMethodName(Expression expression, Type type) {
		assert scopeHasRequiredType(Optional.of(expression), BaseStream.class);

		if (type.isPrimitiveType()) {
			Primitive primitiveType = type.asPrimitiveType().getType();
			if (scopeHasRequiredType(Optional.of(expression), IntStream.class)
					&& primitiveType == PrimitiveType.Primitive.INT
					|| scopeHasRequiredType(Optional.of(expression), LongStream.class)
							&& primitiveType == PrimitiveType.Primitive.LONG
					|| scopeHasRequiredType(Optional.of(expression), DoubleStream.class)
							&& primitiveType == PrimitiveType.Primitive.DOUBLE) {
				return Optional.of(METHOD_MAP);
			} else if (primitiveType == PrimitiveType.Primitive.INT) {
				return Optional.of(METHOD_MAP_TO_INT);
			} else if (primitiveType == PrimitiveType.Primitive.LONG) {
				return Optional.of(METHOD_MAP_TO_LNG);
			} else if (primitiveType == PrimitiveType.Primitive.DOUBLE) {
				return Optional.of(METHOD_MAP_TO_DBL);
			}
		} else if (scopeHasRequiredType(Optional.of(expression), Object.class)) {
			if (scopeHasRequiredType(Optional.of(expression), Stream.class)) {
				// Object and Stream
				return Optional.of(METHOD_MAP);
			} else {
				// Object and Stream
				return Optional.of("mapToObj");
			}
		}

		return Optional.empty();
	}
}
