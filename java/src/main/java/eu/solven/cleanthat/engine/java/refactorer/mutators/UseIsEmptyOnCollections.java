/*
 * Copyright 2023 Solven
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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.pepper.logging.PepperLogHelper;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrate from 'm.size() == 0’ to ’m.isEmpty()'. Works with {@link Collection}, {@link Map} and {@link String}.
 *
 * @author Benoit Lacelle
 */
// https://rules.sonarsource.com/java/RSPEC-1155
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseIsEmptyOnCollections extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	private static final IntegerLiteralExpr ZERO_EXPR = new IntegerLiteralExpr("0");

	@Override
	public String minimalJavaVersion() {
		// java.util.Collection.isEmpty() exists since 1.2
		// java.lang.String.isEmpty() exists since 1.6
		return IJdkVersionConstants.JDK_6;
	}

	@Override
	public String pmdUrl() {
		// https://github.com/pmd/pmd/blob/master/pmd-java/src/main/java/net/sourceforge/pmd/lang/java/rule/bestpractices/UseCollectionIsEmptyRule.java
		return "https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#usecollectionisempty";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseCollectionIsEmpty");
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof BinaryExpr)) {
			return false;
		}
		BinaryExpr binaryExpr = (BinaryExpr) node;
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

		// Check the called method is .size() or .length()
		String calledMethodName = checkmeForIsEmpty.get().getNameAsString();
		if (!"size".equals(calledMethodName)// For Collection.size()
				&& !"length".equals(calledMethodName) // For String.length()
		) {
			LOGGER.debug("Not calling .size() nor .length()");
			return false;
		}
		Expression lengthScope = optLengthScope.get();
		Optional<ResolvedType> type = optResolvedType(lengthScope);

		if (type.isPresent()) {
			boolean localTransformed = checkTypeAndProcess(node, lengthScope, type.get());
			if (localTransformed) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	private boolean checkTypeAndProcess(Node node, Expression lengthScope, ResolvedType type) {
		boolean transformed;
		if (type.isReferenceType()) {
			LOGGER.info("scope={} type={}", lengthScope, type);
			boolean doIt = false;
			ResolvedReferenceType referenceType = type.asReferenceType();
			if (referenceType.getQualifiedName().equals(Collection.class.getName())
					|| referenceType.getQualifiedName().equals(Map.class.getName())
					|| referenceType.getQualifiedName().equals(String.class.getName())) {
				doIt = true;
			} else {
				// Try to load the Class to check if it is a matching sub-type
				try {
					Class<?> clazz = Class.forName(referenceType.getQualifiedName());
					if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)
							|| String.class.isAssignableFrom(clazz)) {
						doIt = true;
					}
				} catch (RuntimeException | ClassNotFoundException e) {
					LOGGER.debug("This class is not available. Can not confirm it is a Colletion/Map/String");
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
