/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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
import java.util.concurrent.atomic.AtomicInteger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.refactorer.function.OnMethodName;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserNodeMutator;
import lombok.extern.slf4j.Slf4j;

/**
 * A single-node (i.e. not the recursive AST) mutator.
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public abstract class AJavaparserNodeMutator extends AJavaparserAstMutator implements IJavaparserNodeMutator {

	private final AtomicInteger nbReplaceIssues = new AtomicInteger();
	private final AtomicInteger nbRemoveIssues = new AtomicInteger();

	@Override
	public int getNbReplaceIssues() {
		return nbReplaceIssues.get();
	}

	@Override
	public int getNbRemoveIssues() {
		return nbRemoveIssues.get();
	}

	protected Optional<Node> replaceNode(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		throw new UnsupportedOperationException("TODO Implement me in overriden classes");
	}

	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		Optional<Node> optReplacement = replaceNode(nodeAndSymbolSolver);

		if (optReplacement.isPresent()) {
			var replacement = optReplacement.get();
			return tryReplace(nodeAndSymbolSolver.getNode(), replacement);
		} else {
			return false;
		}
	}

	protected boolean tryReplace(NodeAndSymbolSolver<?> node, Node replacement) {
		return tryReplace(node.getNode(), replacement);
	}

	protected boolean tryReplace(Node node, Node replacement) {
		if (cancelDueToComment(node)) {
			LOGGER.info("We skip replacing {} due to the presence of a comment", node);
			return false;
		}

		LOGGER.info("{} is turning `{}` into `{}`", getClass().getSimpleName(), node, replacement);

		var result = node.replace(replacement);

		if (!result) {
			nbReplaceIssues.incrementAndGet();
			LOGGER.warn("{} failed turning `{}` into `{}`", getClass().getSimpleName(), node, replacement);
		}

		return result;
	}

	protected boolean tryRemove(Node node) {
		if (cancelDueToComment(node)) {
			LOGGER.info("We skip removing {} due to the presence of a comment", node);
			return false;
		}

		var nodeParentAsString = node.getParentNode().map(n -> n.getClass().getSimpleName()).orElse("-");
		LOGGER.info("Removing `{}` from a {}", node, nodeParentAsString);

		var result = node.remove();

		if (!result) {
			nbRemoveIssues.incrementAndGet();
			LOGGER.warn("Failed removing `{}` from a {}", node, nodeParentAsString);
		}
		return result;
	}

	protected boolean cancelDueToComment(Node node) {
		if (node.findFirst(Node.class, n -> n.getComment().isPresent()).isPresent()) {
			// For now, Cleanthat is pretty weak on comment management (due to Javaparser limitations)
			// So we prefer aborting any modification in case of comment presence, to prevent losing comments
			// https://github.com/javaparser/javaparser/issues/3677
			LOGGER.debug("You should cancel the operation due to the presence of a comment");
			return true;
		}

		return false;
	}

	public static void logJavaParserIssue(Object o, Throwable e, String issue) {
		var msg = "We encounter a case of {} for `{}`. Full-stack is available in 'debug'";
		if (LOGGER.isDebugEnabled()) {
			LOGGER.warn(msg, issue, o, e);
		} else {
			LOGGER.warn(msg, issue, o);
		}
	}

	protected Optional<ResolvedDeclaration> optResolved(Expression expr) {
		if (expr.findCompilationUnit().isEmpty()) {
			// This node is not hooked anymore on a CompilationUnit
			return Optional.empty();
		}

		if (!(expr instanceof Resolvable<?>)) {
			return Optional.empty();
		}

		try {
			Object resolved = ((Resolvable<?>) expr).resolve();
			return Optional.of((ResolvedDeclaration) resolved);
			// resolved = ((Resolvable<ResolvedValueDeclaration>) singleArgument).resolve();
		} catch (UnsolvedSymbolException e) {
			LOGGER.debug("Typically a 3rd-party symbol (e.g. in some library not loaded by CleanThat)", e);
			return Optional.empty();
		} catch (IllegalStateException | UnsupportedOperationException e) {
			if (e.getMessage().contains(SymbolResolver.class.getSimpleName())) {
				// com.github.javaparser.ast.Node.getSymbolResolver()
				LOGGER.debug("Typically a 3rd-party symbol (e.g. in some library not loaded by CleanThat)", e);
				return Optional.empty();
			} else if (e.getMessage().contains("unsolved symbol")) {
				// Caused by: java.lang.UnsupportedOperationException: CorrespondingDeclaration not available for
				// unsolved symbol.
				// at
				// com.github.javaparser.resolution.model.SymbolReference.getCorrespondingDeclaration(SymbolReference.java:116)
				LOGGER.debug("Typically a 3rd-party symbol (e.g. in some library not loaded by CleanThat)", e);
				return Optional.empty();
			} else {
				throw new IllegalStateException(e);
			}
		}
	}

	protected void onMethodName(NodeAndSymbolSolver<?> nodeAndSolver, String methodName, OnMethodName consumer) {
		Node node = nodeAndSolver.getNode();
		if (node instanceof MethodCallExpr && methodName.equals(((MethodCallExpr) node).getName().getIdentifier())) {
			var methodCall = (MethodCallExpr) node;
			Optional<Expression> optScope = methodCall.getScope();
			if (optScope.isEmpty()) {
				// TODO Document when this would happen
				return;
			}
			var scope = optScope.get();
			Optional<ResolvedType> type = MethodCallExprHelpers.optResolvedType(nodeAndSolver.editNode(scope));
			if (type.isPresent()) {
				consumer.onMethodName(methodCall, scope, type.get());
			}
		}
	}

	protected boolean isMethodReturnUsed(MethodCallExpr methodCall) {
		if (!methodCall.getParentNode().isPresent()) {
			return false;
		}

		Node parentNode = methodCall.getParentNode().get();

		if (parentNode instanceof ExpressionStmt) {
			// The method is called, and nothing is done with it
			return false;
		}
		return true;
	}
}
