package eu.solven.cleanthat.rules;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 * 
 * @author Benoit Lacelle
 *
 */
public class PreferConstantsAsEqualsLeftOperator implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public boolean transform(MethodDeclaration pre) {
		pre.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));

			if (node instanceof MethodCallExpr && ("isEmpty".equals(((MethodCallExpr) node).getName().getIdentifier())
					|| "isPresent".equals(((MethodCallExpr) node).getName().getIdentifier()))) {
				MethodCallExpr methodCall = (MethodCallExpr) node;

				Optional<Node> optParent = methodCall.getParentNode();
				// on cherche le '!' logique
				if (optParent.isPresent() && optParent.get() instanceof UnaryExpr
						&& "LOGICAL_COMPLEMENT".equals(((UnaryExpr) optParent.get()).getOperator().name())
						&& methodCall.getScope().isPresent()) {
					// une fois trouvee on fait l'inversion qui convient en recuperant dabord le scope
					Optional<Expression> optScope = methodCall.getScope();
					Expression scope = optScope.get();
					if ("isEmpty".equals(((MethodCallExpr) node).getName().getIdentifier())) {
						MethodCallExpr replacement = new MethodCallExpr(scope, "isPresent");
						LOGGER.info("Turning {} into {}", node, replacement);
						optParent.get().replace(replacement);
					} else {
						MethodCallExpr replacement = new MethodCallExpr(scope, "isEmpty");
						LOGGER.info("Turning {} into {}", node, replacement);
						optParent.get().replace(replacement);
					}
					return;
				}

			}
		});

		return false;
	}
}
