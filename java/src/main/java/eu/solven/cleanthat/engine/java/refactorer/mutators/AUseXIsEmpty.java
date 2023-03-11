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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;

/**
 * Migrate from 'm.size() == 0’ to ’m.isEmpty()'. Works with {@link Collection}, {@link Map} and {@link String}.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public abstract class AUseXIsEmpty extends AJavaparserExprMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(AUseXIsEmpty.class);

	private static final IntegerLiteralExpr ZERO_EXPR = new IntegerLiteralExpr("0");

	protected abstract String getSizeMethod();

	protected abstract Set<Class<?>> getCompatibleTypes();

	@Override
	protected boolean processNotRecursively(Expression expr) {
		Optional<Expression> optLengthScope = checkCallSizeAndCompareWith0(getSizeMethod(), expr);

		if (optLengthScope.isEmpty()) {
			return false;
		}

		var lengthScope = optLengthScope.get();
		Optional<ResolvedType> type = optResolvedType(lengthScope);

		if (type.isPresent()) {
			return checkTypeAndProcess(expr, lengthScope, type.get());
		}

		return false;
	}

	protected Optional<Expression> checkCallSizeAndCompareWith0(String methodName, Node node) {
		if (!(node instanceof BinaryExpr)) {
			return Optional.empty();
		}
		var binaryExpr = (BinaryExpr) node;
		if (!BinaryExpr.Operator.EQUALS.equals(binaryExpr.getOperator())) {
			// We search for 'x == 0' or '0 == x'
			return Optional.empty();
		}

		Optional<MethodCallExpr> checkmeForIsEmpty;
		if (ZERO_EXPR.equals(binaryExpr.getRight()) && binaryExpr.getLeft() instanceof MethodCallExpr) {
			// xxx.method() == 0
			checkmeForIsEmpty = Optional.of((MethodCallExpr) binaryExpr.getLeft());
		} else if (ZERO_EXPR.equals(binaryExpr.getLeft()) && binaryExpr.getRight() instanceof MethodCallExpr) {
			// 0 == xxx.method()
			checkmeForIsEmpty = Optional.of((MethodCallExpr) binaryExpr.getRight());
		} else {
			checkmeForIsEmpty = Optional.empty();
		}
		if (checkmeForIsEmpty.isEmpty()) {
			return Optional.empty();
		}
		Optional<Expression> optLengthScope = checkmeForIsEmpty.get().getScope();
		if (optLengthScope.isEmpty()) {
			return Optional.empty();
		}

		// Check the called method is .size()
		var calledMethodName = checkmeForIsEmpty.get().getNameAsString();
		if (!methodName.equals(calledMethodName)) {
			LOGGER.debug("Not calling `.{}()`", methodName);
			return Optional.empty();
		}
		return optLengthScope;
	}

	protected boolean checkTypeAndProcess(Node node, Expression lengthScope, ResolvedType type) {
		boolean transformed;
		if (type.isReferenceType()) {
			LOGGER.debug("scope={} type={}", lengthScope, type);
			var doIt = false;
			var referenceType = type.asReferenceType();
			if (referenceType.getQualifiedName().equals(String.class.getName())) {
				doIt = true;
			} else {
				// Try to load the Class to check if it is a matching sub-type
				try {
					Class<?> clazz = Class.forName(referenceType.getQualifiedName());
					if (getCompatibleTypes().stream().anyMatch(c -> c.isAssignableFrom(clazz))) {
						doIt = true;
					}
				} catch (RuntimeException | ClassNotFoundException e) {
					LOGGER.debug("This class is not available. Can not confirm it is a String");
				}
			}
			if (doIt) {
				// replace 'x.size() == 0' with 'x.isEmpty()'
				transformed = tryReplace(node, new MethodCallExpr(lengthScope, "isEmpty"));
			} else {
				transformed = false;
			}
		} else {
			transformed = false;
		}
		return transformed;
	}

}
