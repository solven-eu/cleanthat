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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ResolvedTypeHelpers;
import lombok.extern.slf4j.Slf4j;

/**
 * Use the diamond operation {@code <>} whenever possible.
 *
 * @author Benoit Lacelle
 */
// see org.openrewrite.java.cleanup.UseDiamondOperator
@Slf4j
@Deprecated(since = "Not-ready")
public class UseDiamondOperator extends AJavaparserNodeMutator {

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("ExplicitToImplicit");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-2293");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_codestyle.html#usediamondoperator";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseDiamondOperator");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

	// NodeWithTypeArguments
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndSolver) {
		Node node = nodeAndSolver.getNode();
		if (!(node instanceof NodeWithTypeArguments)) {
			return false;
		}
		NodeWithTypeArguments<?> withTypeArgument = (NodeWithTypeArguments<?>) node;

		Optional<Node> optParentNode = node.getParentNode();

		if (optParentNode.isPresent() && optParentNode.get() instanceof ObjectCreationExpr
				&& !withTypeArgument.isUsingDiamondOperator()) {
			var objectCreationExpr = (ObjectCreationExpr) optParentNode.get();
			Optional<ResolvedType> optTypeDeclaration =
					ResolvedTypeHelpers.optResolvedType(objectCreationExpr.getType());
			// objectCreationExpr.calculateResolvedType().asReferenceType().getTypeDeclaration();
			if (optTypeDeclaration.isEmpty()) {
				return false;
			}

			if (!optTypeDeclaration.get().isReferenceType()) {
				return false;
			}

			var asReferenceType = optTypeDeclaration.get().asReferenceType();

			if (asReferenceType.getTypeDeclaration().isEmpty()) {
				return false;
			}

			var asClass = asReferenceType.getTypeDeclaration().get().asClass();
			if (asClass.isAnonymousClass()) {
				// We need the explicit type in the generic type for anonymous class
				return false;
			}

			// TODO Hope for help at https://github.com/javaparser/javaparser/issues/3333
			LOGGER.debug("This change is unsafe");
			// withTypeArgument.setDiamondOperator();
			// return true;
			return false;
		}

		return false;
	}
}
