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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Migrate from 'm.size() == 0’ to ’m.isEmpty()'. Works with {@link Collection}, {@link Map} and {@link String}.
 *
 * @author Benoit Lacelle
 */
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseStringIsEmpty extends AJavaparserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseStringIsEmpty.class);

	private static final IntegerLiteralExpr ZERO_EXPR = new IntegerLiteralExpr("0");

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public String minimalJavaVersion() {
		// java.lang.String.isEmpty() exists since 1.6
		return IJdkVersionConstants.JDK_6;
	}

	@Override
	public Optional<String> getCleanthatId() {
		// Naming similar to UseCollectionIsEmpty
		return Optional.of("UseStringIsEmpty");
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof BinaryExpr)) {
			return false;
		}
		var binaryExpr = (BinaryExpr) node;
		if (!BinaryExpr.Operator.EQUALS.equals(binaryExpr.getOperator())) {
			// We search for 'x == 0' or '0 == x'
			return false;
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
			return false;
		}
		Optional<Expression> optLengthScope = checkmeForIsEmpty.get().getScope();
		if (optLengthScope.isEmpty()) {
			return false;
		}

		// Check the called method is .length()
		var calledMethodName = checkmeForIsEmpty.get().getNameAsString();
		if (!"length".equals(calledMethodName)) {
			LOGGER.debug("Not calling `.length()`");
			return false;
		}
		var lengthScope = optLengthScope.get();
		Optional<ResolvedType> type = optResolvedType(lengthScope);

		if (type.isPresent()) {
			var localTransformed = checkTypeAndProcess(node, lengthScope, type.get());
			if (localTransformed) {
				return true;
			}
		}

		return false;
	}

	private boolean checkTypeAndProcess(Node node, Expression lengthScope, ResolvedType type) {
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
					if (String.class.isAssignableFrom(clazz)) {
						doIt = true;
					}
				} catch (RuntimeException | ClassNotFoundException e) {
					LOGGER.debug("This class is not available. Can not confirm it is a String");
				}
			}
			if (doIt) {
				// replace 'x.size() == 0' with 'x.isEmpty()'
				transformed = node.replace(new MethodCallExpr(lengthScope, "isEmpty"));
			} else {
				transformed = false;
			}
		} else {
			transformed = false;
		}
		return transformed;
	}
}
