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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Turns '{}' into ''
 *
 * @author Benoit Lacelle
 */
// https://github.com/openrewrite/rewrite/blob/main/rewrite-java/src/main/java/org/openrewrite/java/cleanup/EmptyBlockVisitor.java
public class EmptyControlStatement extends AJavaParserMutator implements IMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmptyControlStatement.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public boolean isProductionReady() {
		return false;
	}

	@Override
	public Optional<String> getPmdId() {
		// Used to be 'EmptyStatementBlock'
		return Optional.of("EmptyControlStatement");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#emptycontrolstatement";
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-1116");
	}

	@Override
	public String sonarUrl() {
		return "https://rules.sonarsource.com/java/RSPEC-1116";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof BlockStmt)) {
			return false;
		}
		BlockStmt blockStmt = (BlockStmt) node;

		boolean removed = false;

		// We loop as an empty statement may be wrapped in a (yet-to-come) empty statement
		while (true) {
			// Get parent before removing the children
			Optional<Node> optParentNode = blockStmt.getParentNode();

			if (!blockStmt.getChildNodes().isEmpty()) {
				break;
			}

			removed = blockStmt.remove();

			if (!removed) {
				// For any reason, the removal failed. Break the loop as grandParent would then not be empty
				break;
			}

			if (optParentNode.isPresent() && optParentNode.get() instanceof BlockStmt) {
				blockStmt = (BlockStmt) optParentNode.get();
			} else {
				break;
			}
		}

		if (blockStmt.getChildNodes().isEmpty() && blockStmt.getParentNode().isPresent()) {
			// Removal failure may happen on empty constructor in anonymous class
			LOGGER.debug("Encountered an empty initializer in an anonymous class?");

			if (blockStmt.getParentNode().get() instanceof InitializerDeclaration) {
				InitializerDeclaration parentInitializerDeclaration =
						(InitializerDeclaration) blockStmt.getParentNode().get();

				removed |= parentInitializerDeclaration.remove();
			}
		}

		return removed;
	}
}
