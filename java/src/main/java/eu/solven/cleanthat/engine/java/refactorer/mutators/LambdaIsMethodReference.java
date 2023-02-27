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

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Turns `.stream(s -> s.size())` into `.stream(String::size)`
 *
 * @author Benoit Lacelle
 */
public class LambdaIsMethodReference extends AJavaParserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(LambdaIsMethodReference.class);

	@Override
	public boolean isDraft() {
		// Beware of Objects import management
		return true;
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Optional<String> getCleanthatId() {
		return Optional.of("LambdaIsMethodReference");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("S1612");
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/lambda-to-method-reference.html#code-changes";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.info("{}", PepperLogHelper.getObjectAndClass(node));

		if (!(node instanceof LambdaExpr)) {
			return false;
		}

		LambdaExpr lambdaExpr = (LambdaExpr) node;

		Statement body = lambdaExpr.getBody();

		if (lambdaExpr.getParameters().size() == 1) {
			Parameter singleParameter = lambdaExpr.getParameters().get(0);

			if (body.isExpressionStmt() && body.asExpressionStmt().getExpression().isInstanceOfExpr()) {

				InstanceOfExpr instanceOfExpr = body.asExpressionStmt().getExpression().asInstanceOfExpr();

				if (!instanceOfExpr.getExpression().isNameExpr()
						|| !instanceOfExpr.getExpression().asNameExpr().getName().equals(singleParameter.getName())) {
					return false;
				}

				// `a -> a instanceof B` -> `B.class::isInstance`
				ClassExpr newScope = new ClassExpr(instanceOfExpr.getType());
				MethodReferenceExpr methodReference = new MethodReferenceExpr(newScope, new NodeList<>(), "isInstance");
				return lambdaExpr.replace(methodReference);
			} else if (body.isExpressionStmt() && body.asExpressionStmt().getExpression().isCastExpr()) {
				CastExpr castExpr = body.asExpressionStmt().getExpression().asCastExpr();

				// `a -> (B) a` -> `B.class::cast`
				ClassExpr newScope = new ClassExpr(castExpr.getType());
				MethodReferenceExpr methodReference = new MethodReferenceExpr(newScope, new NodeList<>(), "cast");
				return lambdaExpr.replace(methodReference);

			} else if (body.isExpressionStmt() && body.asExpressionStmt().getExpression().isMethodCallExpr()) {
				MethodCallExpr methodCallExpr = body.asExpressionStmt().getExpression().asMethodCallExpr();

				Optional<Expression> optScope = methodCallExpr.getScope();
				if (optScope.isEmpty()) {
					return false;
				}

				Expression scope = optScope.get();

				Optional<ResolvedType> scopeType = optResolvedType(scope);

				if (scopeType.isEmpty()) {
					return false;
				} else if (!scopeType.get().isReferenceType()) {
					return false;
				}

				// ClassOrInterfaceType type =
				// new ClassOrInterfaceType(scopeType.get().asReferenceType().getQualifiedName());

				// `b -> b.<String>getObject()` -> `B::<String>getObject`
				// MethodReferenceExpr methodReference = new MethodReferenceExpr(new ClassExpr(type),
				// new NodeList<>(),
				// methodCallExpr.getNameAsString());
				// return lambdaExpr.replace(methodReference);

				// `b -> System.out.println(b)` into `System.out::println`
				MethodReferenceExpr methodReference =
						new MethodReferenceExpr(scope, new NodeList<>(), methodCallExpr.getNameAsString());
				return lambdaExpr.replace(methodReference);

			} else if (body.isExpressionStmt() && body.asExpressionStmt().getExpression().isBinaryExpr()) {
				BinaryExpr binaryExpr = body.asExpressionStmt().getExpression().asBinaryExpr();
				Operator operator = binaryExpr.getOperator();
				Expression left = binaryExpr.getLeft();
				Expression right = binaryExpr.getRight();
				if ((operator == Operator.EQUALS || operator == Operator.NOT_EQUALS)
						&& (left.isNullLiteralExpr() || right.isNullLiteralExpr())) {

					Expression notNunull;
					if (left.isNullLiteralExpr()) {
						if (right.isNullLiteralExpr()) {
							// comparing null with null
							return binaryExpr.replace(new BooleanLiteralExpr(true));
						} else {
							notNunull = right;
						}
					} else {
						notNunull = left;
					}

					if (!notNunull.isNameExpr()
							|| !notNunull.asNameExpr().getName().equals(singleParameter.getName())) {
						return false;
					}

					// `a -> a == null` -> `Objects::isNull`
					String methodIdentifier;
					if (operator == Operator.EQUALS) {
						methodIdentifier = "isNull";
					} else {
						assert operator == Operator.NOT_EQUALS;
						methodIdentifier = "nonNull";
					}

					// TODO This lack the additional Import?
					// What if there is already an import to guava Objects?
					MethodReferenceExpr methodReference = new MethodReferenceExpr(
							new TypeExpr(new ClassOrInterfaceType(null, Objects.class.getSimpleName())),
							new NodeList<>(),
							methodIdentifier);
					return lambdaExpr.replace(methodReference);
				}
			}
		}

		return false;
	}
}
