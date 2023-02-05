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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.pepper.logging.PepperLogHelper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns '!o.isEmpty()' into 'o.isPresent()'
 *
 * @author Benoit Lacelle
 */
public class OptionalNotEmpty extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(OptionalNotEmpty.class);

	private static final String METHOD_IS_PRESENT = "isPresent";
	private static final String METHOD_IS_EMPTY = "isEmpty";

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public String getId() {
		return "OptionalNotEmpty";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr methodCall = (MethodCallExpr) node;
		String methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!METHOD_IS_EMPTY.equals(methodCallIdentifier) && !METHOD_IS_PRESENT.equals(methodCallIdentifier)) {
			return false;
		}
		Optional<Node> optParent = methodCall.getParentNode();
		// We looks for a negated expression '!optional.isEmpty()'
		if (methodCall.getScope().isEmpty() || optParent.isEmpty() || !(optParent.get() instanceof UnaryExpr)) {
			return false;
		}
		UnaryExpr unaryExpr = (UnaryExpr) optParent.get();
		if (!"LOGICAL_COMPLEMENT".equals(unaryExpr.getOperator().name())) {
			return false;
		}
		Optional<Expression> optScope = methodCall.getScope();

		if (!scopeHasRequiredType(optScope, Optional.class)) {
			return false;
		}

		Expression scope = optScope.get();
		String newMethod;

		if (METHOD_IS_EMPTY.equals(methodCallIdentifier)) {
			newMethod = METHOD_IS_PRESENT;
		} else {
			newMethod = METHOD_IS_EMPTY;
		}

		boolean localTransformed = false;
		MethodCallExpr replacement = new MethodCallExpr(scope, newMethod);
		LOGGER.info("Turning {} into {}", unaryExpr, replacement);
		if (unaryExpr.replace(replacement)) {
			localTransformed = true;
		}
		// TODO Add a rule to replace such trivial 'if else return'
		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}
}
