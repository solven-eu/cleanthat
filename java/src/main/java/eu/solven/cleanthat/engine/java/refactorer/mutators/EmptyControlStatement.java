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
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns '{}' into ''
 *
 * @author Benoit Lacelle
 */
// https://github.com/openrewrite/rewrite/blob/main/rewrite-java/src/main/java/org/openrewrite/java/cleanup/EmptyBlockVisitor.java
public class EmptyControlStatement extends AJavaparserNodeMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmptyControlStatement.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("ExplicitToImplicit");
	}

	@Override
	public Set<String> getPmdIds() {
		return Set.of("EmptyControlStatement", "EmptyStatementBlock");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#emptycontrolstatement";
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1116");
	}

	@Override
	public Optional<String> getCheckstyleId() {
		return Optional.of("EmptyStatementCheck");
	}

	@Override
	public String checkstyleUrl() {
		return "https://javadoc.io/static/com.puppycrawl.tools/checkstyle/8.37/com/puppycrawl/tools/checkstyle/checks/coding/EmptyStatementCheck.html";
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("RemoveEmptyStatement");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/remove-empty-statement.html";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof BlockStmt)) {
			return false;
		}
		var blockStmt = (BlockStmt) node.getNode();

		var removed = false;

		// We loop as an empty statement may be wrapped in a (yet-to-come) empty statement
		while (true) {
			// Get parent before removing the children
			Optional<Node> optParentNode = blockStmt.getParentNode();

			if (optParentNode.isEmpty()) {
				// The parent has been removed in the meantime
				break;
			}

			var parentNode = optParentNode.get();
			if (!(parentNode instanceof BlockStmt)) {
				break;
			}
			var parentNodeAsBlockStmt = (BlockStmt) parentNode;

			if (!blockStmt.getChildNodes().isEmpty()) {
				break;
			}

			removed = blockStmt.remove();

			if (!removed) {
				// For any reason, the removal failed. Break the loop as grandParent would then not be empty
				break;
			}

			blockStmt = parentNodeAsBlockStmt;
		}

		if (blockStmt.getChildNodes().isEmpty() && blockStmt.getParentNode().isPresent()) {
			// Removal failure may happen on empty constructor in anonymous class
			LOGGER.debug("Encountered an empty initializer in an anonymous class?");

			if (blockStmt.getParentNode().get() instanceof InitializerDeclaration) {
				var parentInitializerDeclaration = (InitializerDeclaration) blockStmt.getParentNode().get();

				removed |= parentInitializerDeclaration.remove();
			}
		}

		return removed;
	}
}
