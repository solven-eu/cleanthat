package eu.solven.cleanthat.language.java.rules.mutators;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.rules.AJavaParserRule;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 *
 * @author Benoit Lacelle
 */
public class OptionalNotEmpty extends AJavaParserRule implements IClassTransformer {

	private static final String METHOD_IS_PRESENT = "isPresent";
	private static final String METHOD_IS_EMPTY = "isEmpty";

	private static final Logger LOGGER = LoggerFactory.getLogger(OptionalNotEmpty.class);

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public String getId() {
		return "OptionalNotEmpty";
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr methodCall = (MethodCallExpr) node;
		String methodCallIdentifier = methodCall.getName().getIdentifier();
		if (!METHOD_IS_EMPTY.equals(methodCallIdentifier) && !METHOD_IS_PRESENT.equals(methodCallIdentifier)) {
			return false;
		}
		Optional<Node> optParent = methodCall.getParentNode();
		// We looks for a negated expression '!optional.isEmpty()'
		if (methodCall.getScope().isEmpty() || optParent.isEmpty() || !(optParent.get() instanceof UnaryExpr)) {
			return false;
		}
		UnaryExpr unaryExpr = (UnaryExpr) optParent.get();
		if (!"LOGICAL_COMPLEMENT".equals(unaryExpr.getOperator().name())) {
			return false;
		}
		Optional<Expression> optScope = methodCall.getScope();
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
		if (type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(Optional.class.getName())) {
			// We are calling 'isEmpty' not on an Optional object
			isCorrectClass = true;
		}
		if (!isCorrectClass) {
			return false;
		}
		Expression scope = optScope.get();
		boolean localTransformed = false;
		if (METHOD_IS_EMPTY.equals(methodCallIdentifier)) {
			MethodCallExpr replacement = new MethodCallExpr(scope, METHOD_IS_PRESENT);
			LOGGER.info("Turning {} into {}", unaryExpr, replacement);
			if (unaryExpr.replace(replacement)) {
				localTransformed = true;
			}
		} else {
			MethodCallExpr replacement = new MethodCallExpr(scope, METHOD_IS_EMPTY);
			LOGGER.info("Turning {} into {}", unaryExpr, replacement);
			if (unaryExpr.replace(replacement)) {
				localTransformed = true;
			}
		}
		// TODO Add a rule to replace such trivial 'if else return'
		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}
}
