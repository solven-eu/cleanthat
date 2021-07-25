package eu.solven.cleanthat.rules;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prefer 'm.isEmpty()' over 'm.size() == 0'
 *
 * @author Benoit Lacelle
 */
// https://rules.sonarsource.com/java/RSPEC-1155
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseIsEmptyOnCollections extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	private static final IntegerLiteralExpr ZERO_EXPR = new IntegerLiteralExpr("0");

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_6;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (node instanceof BinaryExpr && BinaryExpr.Operator.EQUALS.equals(((BinaryExpr) node).getOperator())) {
			BinaryExpr binaryExpr = (BinaryExpr) node;
			Optional<MethodCallExpr> checkmeForIsEmpty;
			if (binaryExpr.getRight().equals(ZERO_EXPR) && binaryExpr.getLeft() instanceof MethodCallExpr) {
				checkmeForIsEmpty = Optional.of((MethodCallExpr) binaryExpr.getLeft());
			} else if (binaryExpr.getLeft().equals(ZERO_EXPR) && binaryExpr.getRight() instanceof MethodCallExpr) {
				checkmeForIsEmpty = Optional.of((MethodCallExpr) binaryExpr.getRight());
			} else {
				checkmeForIsEmpty = Optional.empty();
			}
			if (checkmeForIsEmpty.isEmpty()) {
				return false;
			}
			Optional<Expression> optLengthScope = checkmeForIsEmpty.get().getScope();
			if (optLengthScope.isEmpty()) {
				return false;
			}
			if (!"size".equals(checkmeForIsEmpty.get().getNameAsString())
					&& !"length".equals(checkmeForIsEmpty.get().getNameAsString())) {
				LOGGER.debug("Not calling .size()");
				return false;
			}
			Expression lengthScope = optLengthScope.get();
			Optional<ResolvedType> type = optResolvedType(lengthScope);

			if (type.isPresent()) {
				boolean localTransformed = process(node, lengthScope, type.get());
				if (localTransformed) {
					return true;
				}
			}
		}

		return false;
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	private boolean process(Node node, Expression lengthScope, ResolvedType type) {
		boolean transformed;
		if (type.isReferenceType()) {
			LOGGER.info("scope={} type={}", lengthScope, type);
			boolean doIt = false;
			ResolvedReferenceType referenceType = type.asReferenceType();
			if (referenceType.getQualifiedName().equals(Collection.class.getName())
					|| referenceType.getQualifiedName().equals(Map.class.getName())
					|| referenceType.getQualifiedName().equals(String.class.getName())) {
				doIt = true;
			} else {
				// Try to load the Class to check if it is a matching sub-type
				try {
					Class<?> clazz = Class.forName(referenceType.getQualifiedName());
					if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)
							|| String.class.isAssignableFrom(clazz)) {
						doIt = true;
					}
				} catch (RuntimeException | ClassNotFoundException e) {
					LOGGER.debug("This class is not available. Can not confirm it is a Colletion/Map/String");
				}
			}
			if (doIt) {
				transformed = node.replace(new MethodCallExpr(lengthScope, "isEmpty"));
			} else {
				transformed = false;
			}
		} else {
			transformed = false;
		}
		return transformed;
	}
}
