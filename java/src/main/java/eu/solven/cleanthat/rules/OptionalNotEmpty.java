package eu.solven.cleanthat.rules;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 *
 * @author Benoit Lacelle
 */
public class OptionalNotEmpty extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OptionalNotEmpty.class);

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public boolean walkNode(Node pre) {
		AtomicBoolean transformed = new AtomicBoolean();

		pre.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));

			if (!(node instanceof MethodCallExpr)) {
				return;
			}
			MethodCallExpr methodCall = (MethodCallExpr) node;
			String methodCallIdentifier = methodCall.getName().getIdentifier();

			if (!"isEmpty".equals(methodCallIdentifier) && !"isPresent".equals(methodCallIdentifier)) {
				return;
			}

			Optional<Node> optParent = methodCall.getParentNode();
			// on cherche le '!' logique
			if (methodCall.getScope().isPresent() && optParent.isPresent() && optParent.get() instanceof UnaryExpr) {

				UnaryExpr unaryExpr = (UnaryExpr) optParent.get();
				if (!"LOGICAL_COMPLEMENT".equals(unaryExpr.getOperator().name())) {
					return;
				}

				// une fois trouvee on fait l'inversion qui convient en recuperant dabord le scope
				Optional<Expression> optScope = methodCall.getScope();
				Expression scope = optScope.get();

				boolean localTransformed = false;
				if ("isEmpty".equals(methodCallIdentifier)) {
					MethodCallExpr replacement = new MethodCallExpr(scope, "isPresent");
					LOGGER.info("Turning {} into {}", node, replacement);
					if (optParent.get().replace(replacement)) {
						localTransformed = true;
					}
				} else {
					MethodCallExpr replacement = new MethodCallExpr(scope, "isEmpty");
					LOGGER.info("Turning {} into {}", node, replacement);
					if (optParent.get().replace(replacement)) {
						localTransformed = true;
					}
				}

				if (localTransformed) {
					transformed.set(true);
				}
				return;
			}
		});

		return transformed.get();
	}
}
