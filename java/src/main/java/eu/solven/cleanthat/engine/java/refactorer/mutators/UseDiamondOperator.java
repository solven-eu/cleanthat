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
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.engine.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.engine.java.refactorer.meta.IRuleExternalUrls;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use the diamond operation {@code <>} whenever possible.
 *
 * @author Benoit Lacelle
 */
@Deprecated(since = "Not-ready")
public class UseDiamondOperator extends AJavaParserRule implements IClassTransformer, IRuleExternalUrls {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseDiamondOperator.class);

	@Override
	public boolean isProductionReady() {
		return false;
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public String sonarUrl() {
		return "https://rules.sonarsource.com/java/RSPEC-2293";
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#usediamondoperator";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseDiamondOperator");
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

	// NodeWithTypeArguments
	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof NodeWithTypeArguments)) {
			return false;
		}
		NodeWithTypeArguments<?> withTypeArgument = (NodeWithTypeArguments<?>) node;

		Optional<Node> optParentNode = node.getParentNode();

		if (optParentNode.isPresent() && optParentNode.get() instanceof ObjectCreationExpr
				&& !withTypeArgument.isUsingDiamondOperator()) {
			ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) optParentNode.get();
			Optional<ResolvedReferenceTypeDeclaration> optTypeDeclaration =
					objectCreationExpr.calculateResolvedType().asReferenceType().getTypeDeclaration();
			if (optTypeDeclaration.isEmpty()) {
				return false;
			}

			ResolvedClassDeclaration asClass = optTypeDeclaration.get().asClass();
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
