package eu.solven.cleanthat.rules;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.meta.IRuleExternalUrls;

/**
 * Use the diamond operation '<>' whenever possible.
 *
 * @author Benoit Lacelle
 */
@Deprecated(since = "Not-ready")
public class UseDiamondOperator extends AJavaParserRule implements IClassTransformer, IRuleExternalUrls {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseDiamondOperator.class);

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
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/diamond-operator.html";
	}

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
