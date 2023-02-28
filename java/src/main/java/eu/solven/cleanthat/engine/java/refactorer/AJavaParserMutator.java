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
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.google.common.annotations.VisibleForTesting;

import eu.solven.cleanthat.engine.java.refactorer.function.OnMethodName;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutatorExternalReferences;

/**
 * Enables common behavior to JavaParser-based rules
 *
 * @author Benoit Lacelle
 */
public abstract class AJavaParserMutator implements IJavaparserMutator, IMutatorExternalReferences {
	private static final Logger LOGGER = LoggerFactory.getLogger(AJavaParserMutator.class);

	private static final AtomicInteger WARNS_IDEMPOTENCY_COUNT = new AtomicInteger();

	private static final ThreadLocal<TypeSolver> TL_TYPESOLVER = ThreadLocal.withInitial(() -> {
		return JavaRefactorer.makeDefaultTypeSolver(false);
	});

	private static final ThreadLocal<JavaParserFacade> TL_JAVAPARSER = ThreadLocal.withInitial(() -> {
		// We allow processing any type available to default classLoader
		return JavaParserFacade.get(TL_TYPESOLVER.get());
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
	public Optional<Node> walkAst(Node tree) {
		AtomicBoolean transformed = new AtomicBoolean();
		tree.walk(node -> {
			boolean hasTransformed;
			try {
				hasTransformed = processNotRecursively(node);
			} catch (RuntimeException e) {
				String rangeInSourceCode = "Around lines: " + node.getTokenRange().map(Object::toString).orElse("-");
				String messageForIssueReporting = messageForIssueReporting(this, node);
				throw new IllegalArgumentException(
						"Issue with a cleanthat mutator. " + rangeInSourceCode + " " + messageForIssueReporting,
						e);
			}

			if (hasTransformed) {
				idempotencySanityCheck(node);
				transformed.set(true);
			}
		});
		if (transformed.get()) {
			return Optional.of(tree);
		} else {
			return Optional.empty();
		}
	}

	private void idempotencySanityCheck(Node node) {
		boolean transformAgain = processNotRecursively(node);
		// 'NoOp' is a special parserRule which always returns true even while it did not transform the code
		if (!this.getIds().contains(IMutator.ID_NOOP) && transformAgain) {
			// This may restore the initial code (e.g. if the rule is switching 'a.equals(b)' to 'b.equals(a)'
			// to again 'a.equals(b)')
			WARNS_IDEMPOTENCY_COUNT.incrementAndGet();
			String messageForIssueReporting = messageForIssueReporting(this, node);
			LOGGER.warn("A mutator is not idem-potent. {}", messageForIssueReporting);
		}
	}

	public static String messageForIssueReporting(IMutator mutator, Node node) {
		String faultyCode = node.toString();

		String messageForIssueReporting =
				"\r\n\r\nPlease report it to '" + "https://github.com/solven-eu/cleanthat/issues"
						+ "' referring the faulty mutator: '"
						+ mutator.getClass().getName()
						+ " with as testCase: \r\n\r\n"
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
		LOGGER.info("Turning `{}` into `{}`", node, replacement);

		return node.replace(replacement);
	}

	protected Optional<Node> replaceNode(Node node) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}

	// https://github.com/javaparser/javaparser/issues/1491
	protected Optional<ResolvedType> optResolvedType(Expression expr) {
		try {
			return Optional.of(expr.calculateResolvedType());
		} catch (RuntimeException e) {
			try {
				Optional<ResolvedType> fallbackType = Optional.of(getThreadJavaParser().getType(expr));
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
			logNoClassDefFoundResolvingType(expr, e);

			return Optional.empty();
		}
	}

	private void logNoClassDefFoundResolvingType(Object o, NoClassDefFoundError e) {
		// https://github.com/javaparser/javaparser/issues/3504
		LOGGER.warn("We encounter a case of {} for {}. Full-stack is available in 'debug'",
				"https://github.com/javaparser/javaparser/issues/3504",
				o);
		LOGGER.debug("We encounter a case of {} for {}. Full-stack is available in 'debug'",
				"https://github.com/javaparser/javaparser/issues/3504",
				o,
				e);
	}

	protected Optional<ResolvedType> optResolvedType(Type type) {
		try {
			return Optional.of(type.resolve());
		} catch (RuntimeException e) {
			try {
				TypeSolver symbolSolver = TL_TYPESOLVER.get();
				JavaSymbolSolver symbolResolver = new JavaSymbolSolver(symbolSolver);
				Optional<ResolvedType> fallbackType =
						Optional.of(symbolResolver.toResolvedType(type, ResolvedType.class));
				if (fallbackType.isPresent()) {
					// TODO Is this related to code-modifications?
					LOGGER.debug("1- Does this still happen? As of ???: Yes!", e);
				}
				return fallbackType;
			} catch (UnsolvedSymbolException ee) {
				LOGGER.debug("Issue with JavaParser over {}", type, ee);
				return Optional.empty();
			} catch (RuntimeException ee) {
				throw new IllegalArgumentException(ee);
			}
		} catch (NoClassDefFoundError e) {
			logNoClassDefFoundResolvingType(type, e);

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
