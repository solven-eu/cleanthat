/*
 * Copyright 2023-2026 Benoit Lacelle - SOLVEN
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

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.SymbolResolver;

import eu.solven.cleanthat.SuppressCleanthat;
import eu.solven.cleanthat.engine.java.refactorer.meta.ICountMutatorIssues;
import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserAstMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.pepper.core.PepperLogHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Enables common behavior to JavaParser-based rules
 *
 * @author Benoit Lacelle
 */
@Slf4j
@SuppressWarnings("PMD.GodClass")
public abstract class AJavaparserAstMutator implements IJavaparserAstMutator, ICountMutatorIssues {

	// Some mutator may edit thr input Node, before cancelling the operation: it would leave the input Node in an
	// inconsistent state. To workaround this, we tried first simulating the operation on a cloned Node, but it led to
	// various issues:
	// 1- Type resolution is difficult on cloned Node: we tried replacing the original Node by the Clone, but is lead to
	// issue on IMutator editing the parent of current Node. We also observed additional issues related to
	// LexicalPreservingPrinter
	private static final boolean SIMULATE_BEFORE_EXECUTE = Boolean.getBoolean("cleanthat.simulate_before_execute");

	private final AtomicInteger nbIdempotencyIssues = new AtomicInteger();

	@Override
	public int getNbIdempotencyIssues() {
		return nbIdempotencyIssues.get();
	}

	protected abstract boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSymbolSolver);

	@Override
	public Optional<Node> walkAst(Node ast) {
		var astHasMutated = new AtomicBoolean();

		ast.walk(node -> {
			boolean nodeHasMutated = walkOneNode(node);
			if (nodeHasMutated) {
				astHasMutated.set(true);
			}
		});

		if (astHasMutated.get()) {
			return Optional.of(ast);
		} else {
			return Optional.empty();
		}
	}

	private boolean walkOneNode(Node node) {
		if (node.findCompilationUnit().isEmpty()) {
			LOGGER.debug("We skip {} as it or one of its ancestor has been dropped from the AST", node);
			return false;
		}
		{
			Optional<NodeWithAnnotations> optSuppressedParent =
					node.findAncestor(n -> n.isAnnotationPresent(SuppressCleanthat.class), NodeWithAnnotations.class);
			Optional<Node> optSuppressedChildren = node.findFirst(Node.class,
					n -> n instanceof NodeWithAnnotations<?>
							&& ((NodeWithAnnotations<?>) n).isAnnotationPresent(SuppressCleanthat.class));
			if (node instanceof NodeWithAnnotations
					&& ((NodeWithAnnotations<?>) node).isAnnotationPresent(SuppressCleanthat.class)
					|| optSuppressedParent.isPresent()
					|| optSuppressedChildren.isPresent()) {
				LOGGER.debug("We skip {} due to {}", node, SuppressCleanthat.class.getName());
				return false;
			}
		}

		CompilationUnit compilationUnit = node.findCompilationUnit().get();

		// This requires the node to have a compilationUnit
		SymbolResolver symbolSolver = node.getSymbolResolver();

		// First, we simulate the change over a clone
		// This is done on a clone as some intermediate step (like creating a MethodCallExpr) would change some parent
		// While the IMutator is still free to cancel the mutation
		final boolean hasTransformedClone;
		if (SIMULATE_BEFORE_EXECUTE) {
			hasTransformedClone = simulateOnClone(node, symbolSolver, compilationUnit);
		} else {
			hasTransformedClone = true;
		}

		if (hasTransformedClone) {
			return executeOnNode(node, symbolSolver, compilationUnit);
		} else {
			return false;
		}
	}

	protected boolean simulateOnClone(Node node, SymbolResolver symbolSolver, CompilationUnit compilationUnit) {
		Optional<Node> optParent = node.getParentNode();
		if (optParent.isEmpty()) {
			// Happens when processing a CompilationUnit
			// Skipped for now
			return false;
		}

		// So we attach its to the real parent, in order to enable type resolution, and try its replacement
		// In fact, we attach it to a clone parent, as the final
		Node parentNode = optParent.get();

		Optional<Node> optGrandParent = parentNode.getParentNode();
		if (optGrandParent.isEmpty()) {
			return false;
		}

		// For a successful replacement of current node, we need it to be a proper child of its parent: we need to clone
		// the parent so the child is a real child of its parent.
		Node clonedParentNode = parentNode.clone();

		restoreRealNode(clonedParentNode, optGrandParent, parentNode);

		var indexInParent = getIndexInParent(node, parentNode);

		// The clone has no compilationUnit, hence no symbolResolver
		Node clonedNode = clonedParentNode.getChildNodes().get(indexInParent);

		boolean hasTransformedClone;
		try {

			NodeAndSymbolSolver<Node> clonedNodeAndSymbolSolver = new NodeAndSymbolSolver<>(clonedNode,
					symbolSolver,
					compilationUnit,
					compilationUnit.getPackageDeclaration(),
					compilationUnit.getImports());

			try {
				LOGGER.trace("{} is going over {}",
						this.getClass().getSimpleName(),
						PepperLogHelper.getObjectAndClass(clonedNode));
				hasTransformedClone = processNotRecursively(clonedNodeAndSymbolSolver);
			} catch (RuntimeException e) {
				String rangeInSourceCode = "Around lines: " + node.getTokenRange().map(Object::toString).orElse("-");
				var messageForIssueReporting = messageForIssueReporting(this, clonedNode);
				throw new IllegalArgumentException(
						"Issue with a cleanthat mutator. " + rangeInSourceCode + " " + messageForIssueReporting,
						e);
			}

			boolean nodeEqualsPostClone = node.equals(clonedNode);
			if (hasTransformedClone && nodeEqualsPostClone) {
				// We did not clone
				LOGGER.warn("{} indicates it mutated `{}` but we observe no change", this, node);
			} else if (!hasTransformedClone && !nodeEqualsPostClone) {
				LOGGER.debug(
						"This typically happens as we built MethodCallExpr (changing some parent) but finally discarded the change");
			}
		} finally {
			restoreRealNode(parentNode, optGrandParent, clonedParentNode);
		}

		return hasTransformedClone;
	}

	@SuppressWarnings("PMD.DoNotThrowExceptionInFinally")
	private void restoreRealNode(Node parentNode, Optional<Node> optGrandParent, Node clonedParentNode) {
		// Remove the clonedNode from the CompilationUnit as it was a transient state to enable type resolution
		if (!optGrandParent.get().replace(clonedParentNode, parentNode)) {
			throw new IllegalStateException("Issue replacing Node by its clone");
		}
	}

	@SuppressWarnings("PMD.CompareObjectsWithEquals")
	private int getIndexInParent(Node node, Node parentNode) {
		var indexInParent = -1;
		for (var i = 0; i < parentNode.getChildNodes().size(); i++) {
			if (node == parentNode.getChildNodes().get(i)) {
				indexInParent = i;
				break;
			}
		}

		if (indexInParent < 0) {
			throw new IllegalStateException("Can not find a child amongst its parent children");
		}
		return indexInParent;
	}

	private boolean executeOnNode(Node node, SymbolResolver symbolSolver, CompilationUnit compilationUnit) {
		NodeAndSymbolSolver<Node> nodeAndSymbolSolver = new NodeAndSymbolSolver<>(node,
				symbolSolver,
				compilationUnit,
				compilationUnit.getPackageDeclaration(),
				compilationUnit.getImports());

		final boolean hasTransformedNode;
		try {
			LOGGER.trace("{} is going over {}",
					this.getClass().getSimpleName(),
					PepperLogHelper.getObjectAndClass(node));
			hasTransformedNode = processNotRecursively(nodeAndSymbolSolver);
		} catch (RuntimeException e) {
			String rangeInSourceCode = "Around lines: " + node.getTokenRange().map(Object::toString).orElse("-");
			var messageForIssueReporting = messageForIssueReporting(this, node);
			throw new IllegalArgumentException(
					"Issue with a cleanthat mutator. " + rangeInSourceCode + " " + messageForIssueReporting,
					e);
		}

		if (node.findCompilationUnit().isEmpty()) {
			// The node (or one of its ancestor) has been removed from the compilation unit
			// We supposed another node has been inserted somewhere in replacement
			Optional<Node> broken = compilationUnit.findFirst(Node.class, n -> n.findCompilationUnit().isEmpty());
			if (broken.isPresent()) {
				LOGGER.warn("{} has corrupted the AST from `{}` around `{}`", this.getClass(), node, broken.get());
			}
		} else {
			// This sanityCheck is the reason why we first try the mutation on a clone: given we apply the mutation on a
			// real node only if accepted on a clone, the mutation on the real node should always be accepted, and then
			// it should not corrupt nodes by creating intermediate Nodes
			Optional<Node> broken = node.findFirst(Node.class, n -> n.findCompilationUnit().isEmpty());
			if (broken.isPresent()) {
				LOGGER.warn("{} has corrupted the AST from `{}` around `{}`", this.getClass(), node, broken.get());
			}
		}

		if (hasTransformedNode) {
			LOGGER.debug("{} transformed something into `{}`", this.getClass(), node);

			idempotencySanityCheck(nodeAndSymbolSolver);

			return true;
		} else {
			if (SIMULATE_BEFORE_EXECUTE) {
				LOGGER.warn("A mutation has been rejected while it was accept on a duplicated node");
			}
			return false;
		}
	}

	private void idempotencySanityCheck(NodeAndSymbolSolver<?> nodeAndSymbolSolver) {
		if (this.getIds().contains(IMutator.ID_NOOP)) {
			// 'NoOp' is a special parserRule which always returns true even while it did not transform the code
			return;
		}

		Node node = nodeAndSymbolSolver.getNode();
		if (node.getParentNode().isEmpty()) {
			// This node has seemingly been removed from its parent
			return;
		} else if (node.findCompilationUnit().isEmpty()) {
			// This node has no compilation unit: either it was not in a compilationUnit from the start (e.g. in a
			// unitTest)
			// or it is ancestor which has been replaced (e.g. we analyzed the ancestors to decide to remove an
			// ancestor: current node still has a parent, but no compilationUnit anymore)
			return;
		}

		var transformAgain = processNotRecursively(nodeAndSymbolSolver);
		if (transformAgain) {
			// This may restore the initial code (e.g. if the rule is switching 'a.equals(b)' to 'b.equals(a)'
			// to again 'a.equals(b)')
			nbIdempotencyIssues.incrementAndGet();
			var messageForIssueReporting = messageForIssueReporting(this, nodeAndSymbolSolver.getNode());
			LOGGER.warn("A mutator is not idem-potent. {}", messageForIssueReporting);
		}
	}

	public static String messageForIssueReporting(IMutator mutator, Node node) {
		var faultyCode = node.toString();

		String messageForIssueReporting =
				"\r\n\r\nPlease report it to '" + "https://github.com/solven-eu/cleanthat/issues"
						+ "' referring the faulty mutator: '"
						+ mutator.getClass().getName()
						+ " with as testCase: \r\n\r\n"
						+ faultyCode;
		return messageForIssueReporting;
	}
}
