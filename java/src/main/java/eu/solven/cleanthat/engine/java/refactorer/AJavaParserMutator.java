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
package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.annotations.VisibleForTesting;

import eu.solven.cleanthat.engine.java.refactorer.function.OnMethodName;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IRuleExternalReferences;

/**
 * Enables common behavior to JavaParser-based rules
 *
 * @author Benoit Lacelle
 */
public abstract class AJavaParserMutator implements IJavaparserMutator, IRuleExternalReferences {
	private static final Logger LOGGER = LoggerFactory.getLogger(AJavaParserMutator.class);

	private static final AtomicInteger WARNS_IDEMPOTENCY_COUNT = new AtomicInteger();

	private static final ThreadLocal<JavaParserFacade> TL_JAVAPARSER = ThreadLocal.withInitial(() -> {
		CombinedTypeSolver ts = new CombinedTypeSolver();

		// We allow processing any type available to default classLoader
		boolean jreOnly = false;

		ts.add(new ReflectionTypeSolver(jreOnly));
		return JavaParserFacade.get(ts);
	});

	@Deprecated
	@VisibleForTesting
	public static int getWarnCount() {
		return WARNS_IDEMPOTENCY_COUNT.get();
	}

	protected final JavaParserFacade getThreadJavaParser() {
		return TL_JAVAPARSER.get();
	}

	@Override
	public boolean walkNode(Node tree) {
		AtomicBoolean transformed = new AtomicBoolean();
		tree.walk(node -> {
			boolean hasTransformed;
			try {
				hasTransformed = processNotRecursively(node);
			} catch (RuntimeException e) {
				String messageForIssueReporting = messageForIssueReporting(this, node);
				throw new IllegalArgumentException("Issue with a cleanthat mutator. " + messageForIssueReporting, e);
			}

			if (hasTransformed) {
				idempotencySanityCheck(node);
				transformed.set(true);
			}
		});
		return transformed.get();
	}

	private void idempotencySanityCheck(Node node) {
		boolean transformAgain = processNotRecursively(node);
		// 'NoOp' is a special parserRule which always returns true even while it did not transform the code
		if (!this.getIds().contains(IMutator.ID_NOOP) && transformAgain) {
			// This may restore the initial code (e.g. if the rule is switching 'a.equals(b)' to 'b.equals(a)'
			// to again 'a.equals(b)')
			WARNS_IDEMPOTENCY_COUNT.incrementAndGet();
			String messageForIssueReporting = messageForIssueReporting(this, node);
			LOGGER.warn("Applying {} over {} is not idem-potent. It is a bug! {}",
					this,
					node,
					messageForIssueReporting);
		}
	}

	public static String messageForIssueReporting(IMutator mutator, Node node) {
		String faultyCode = node.toString();

		String messageForIssueReporting = "Please report it to '" + "https://github.com/solven-eu/cleanthat/issues"
				+ "' referring the faulty mutator: '"
				+ mutator.getClass().getName()
				+ "' with as testCase: \r\n\r\n"
				+ faultyCode;
		return messageForIssueReporting;
	}

	protected boolean processNotRecursively(Node node) {
		Optional<Node> optReplacement = replaceNode(node);

		if (optReplacement.isPresent()) {
			Node replacement = optReplacement.get();
			return tryReplace(node, replacement);
		} else {
			return false;
		}
	}

	public boolean tryReplace(Node node, Node replacement) {
		LOGGER.info("Turning {} into {}", node, replacement);

		return node.replace(replacement);
	}

	protected Optional<Node> replaceNode(Node node) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}

	protected Optional<ResolvedType> optResolvedType(Expression scope) {
		ResolvedType calculateResolvedType;
		try {
			calculateResolvedType = scope.calculateResolvedType();
		} catch (RuntimeException e) {
			try {
				Optional<ResolvedType> fallbackType = Optional.of(getThreadJavaParser().getType(scope));
				if (fallbackType.isPresent()) {
					// TODO Is this related to code-modifications?
					LOGGER.debug("1- Does this still happen? As of 2022-12: Yes!", e);
				}
				return fallbackType;
			} catch (RuntimeException ee) {
				LOGGER.debug("Issue with JavaParser: {} {}", ee.getClass().getName(), ee.getMessage());
				return Optional.empty();
			}
		} catch (NoClassDefFoundError e) {
			// https://github.com/javaparser/javaparser/issues/3504
			LOGGER.warn("We encounter a case of {} for {}. Full-stack is available in 'debug'",
					"https://github.com/javaparser/javaparser/issues/3504",
					scope);
			LOGGER.debug("We encounter a case of {} for {}. Full-stack is available in 'debug'",
					"https://github.com/javaparser/javaparser/issues/3504",
					scope,
					e);

			return Optional.empty();
		}
		try {
			ResolvedType manualResolvedType = getThreadJavaParser().getType(scope);

			if (!manualResolvedType.toString().equals(calculateResolvedType.toString())) {
				throw new IllegalStateException(
						manualResolvedType.toString() + " not equals to " + calculateResolvedType.toString());
			}
			// return Optional.of(calculateResolvedType);
			return Optional.of(manualResolvedType);
		} catch (RuntimeException e) {
			LOGGER.warn("2- Does this still happen? Yes!", e);
			// This will happen often as, as of 2021-01, we solve types only given current class context,
			// neither other classes of the project, nor its maven/gradle dependencies
			// UnsolvedSymbolException
			// https://github.com/javaparser/javaparser/issues/1491
			LOGGER.debug("Issue with JavaParser: {} {}", e.getClass().getName(), e.getMessage());
			return Optional.empty();
		}
	}

	protected void onMethodName(Node node, String methodName, OnMethodName consumer) {
		if (node instanceof MethodCallExpr && methodName.equals(((MethodCallExpr) node).getName().getIdentifier())) {
			MethodCallExpr methodCall = (MethodCallExpr) node;
			Optional<Expression> optScope = methodCall.getScope();
			if (optScope.isEmpty()) {
				// TODO Document when this would happen
				return;
			}
			Expression scope = optScope.get();
			Optional<ResolvedType> type = optResolvedType(scope);
			if (type.isPresent()) {
				consumer.onMethodName(methodCall, scope, type.get());
			}
		}
	}

	protected boolean scopeHasRequiredType(Optional<Expression> optScope, Class<?> requiredType) {
		return scopeHasRequiredType(optScope, requiredType.getName());
	}

	protected boolean scopeHasRequiredType(Optional<Expression> optScope, String requiredType) {
		Optional<ResolvedType> optType = optScope.flatMap(this::optResolvedType);
		if (optType.isEmpty()) {
			return false;
		}
		ResolvedType type = optType.get();
		boolean isCorrectClass = false;
		if (type.isConstraint()) {
			// Happens on Lambda
			type = type.asConstraintType().getBound();
		}
		if (type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(requiredType)) {
			// We are calling 'isEmpty' not on an Optional object
			isCorrectClass = true;
		} else if (type.isPrimitive() && type.asPrimitive().describe().equals(requiredType)) {
			// For a primitive double, requiredType is 'double'
			isCorrectClass = true;
		}
		if (!isCorrectClass) {
			return false;
		}

		return true;
	}
}
