package eu.solven.cleanthat.rules;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 * 
 * @author Benoit Lacelle
 *
 */
public class ReplaceOptionalNotEmpty implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	private static final IntegerLiteralExpr ZERO_EXPR = new IntegerLiteralExpr("0");

	// Optional exists since 8
	// Optional.isPresent exists since 11
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
	}

	@Override
	public void transform(MethodDeclaration pre) {
		CombinedTypeSolver ts = new CombinedTypeSolver();
		ts.add(new ReflectionTypeSolver());
		JavaParserFacade javaParserFacade = JavaParserFacade.get(ts);

		pre.walk(node -> {
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
					return;
				}

				Optional<Expression> optLengthScope = checkmeForIsEmpty.get().getScope();
				if (optLengthScope.isEmpty()) {
					return;
				}

				if (!"size".equals(checkmeForIsEmpty.get().getNameAsString())
						&& !"length".equals(checkmeForIsEmpty.get().getNameAsString())) {
					LOGGER.debug("Not calling .size()");
					return;
				}

				Expression lengthScope = optLengthScope.get();
				ResolvedType type;
				try {
					type = javaParserFacade.getType(lengthScope);
				} catch (RuntimeException e) {
					LOGGER.debug("ARG", e);
					LOGGER.info("ARG solving type of scope: {}", optLengthScope);
					return;
					// throw new IllegalStateException("Issue on scope=" + scope, e);
				}

				process(node, lengthScope, type);
			}
		});
	}

	private void process(Node node, Expression lengthScope, ResolvedType type) {
		if (type.isReferenceType()) {
			LOGGER.info("scope={} type={}", lengthScope, type);

			boolean doIt = false;
			if (type.asReferenceType().getQualifiedName().equals(Collection.class.getName())
					|| type.asReferenceType().getQualifiedName().equals(Map.class.getName())
					|| type.asReferenceType().getQualifiedName().equals(String.class.getName())) {
				doIt = true;
			} else {
				// Try to load the Class to check if it is a matching sub-type
				try {
					Class<?> clazz = Class.forName(type.asReferenceType().getQualifiedName());

					if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)
							|| String.class.isAssignableFrom(clazz)) {
						doIt = true;
					}
				} catch (RuntimeException | ClassNotFoundException e) {
					LOGGER.debug("This class is not available. Can not confirm it is a Colletion/Map/String");
				}
			}

			if (doIt) {
				node.replace(new MethodCallExpr(lengthScope, "isEmpty"));
			}
		}
	}
}