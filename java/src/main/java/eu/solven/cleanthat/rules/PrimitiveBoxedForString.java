package eu.solven.cleanthat.rules;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Clean the way of converting primitives into {@link String}.
 * 
 * @author Benoit Lacelle
 *
 */
// https://jsparrow.github.io/rules/primitive-boxed-for-string.html
// https://rules.sonarsource.com/java/RSPEC-1158
public class PrimitiveBoxedForString implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PrimitiveBoxedForString.class);

	public String minimalJavaVersion() {
		return "1.1";
	}

	public void transform(MethodDeclaration tree) {
		CombinedTypeSolver ts = new CombinedTypeSolver();
		ts.add(new ReflectionTypeSolver());
		JavaParserFacade javaParserFacade = JavaParserFacade.get(ts);

		tree.walk(node -> {
			LOGGER.info("{}", PepperLogHelper.getObjectAndClass(node));

			if (node instanceof MethodCallExpr
					&& "toString".equals(((MethodCallExpr) node).getName().getIdentifier())) {
				MethodCallExpr methodCall = ((MethodCallExpr) node);
				Optional<Expression> optScope = methodCall.getScope();
				if (!optScope.isPresent()) {
					// TODO Document when this would happen
					return;
				}
				Expression scope = optScope.get();

				ResolvedType type;
				try {
					type = javaParserFacade.getType(scope);
				} catch (RuntimeException e) {
					LOGGER.debug("ARG", e);
					LOGGER.info("ARG solving type of scope: {}", scope);
					return;
					// throw new IllegalStateException("Issue on scope=" + scope, e);
				}

				if (type.isReferenceType()) {
					if (Boolean.class.getName().equals(type.asReferenceType().getQualifiedName())
							|| Byte.class.getName().equals(type.asReferenceType().getQualifiedName())
							|| Short.class.getName().equals(type.asReferenceType().getQualifiedName())
							|| Integer.class.getName().equals(type.asReferenceType().getQualifiedName())
							|| Long.class.getName().equals(type.asReferenceType().getQualifiedName())
							|| Float.class.getName().equals(type.asReferenceType().getQualifiedName())
							|| Double.class.getName().equals(type.asReferenceType().getQualifiedName())) {
						if (scope instanceof ObjectCreationExpr) {
							ObjectCreationExpr creation = (ObjectCreationExpr) scope;

							NodeList<Expression> inputs = creation.getArguments();

							MethodCallExpr replacement =
									new MethodCallExpr(new NameExpr(creation.getType().getName()), "toString", inputs);
							LOGGER.info("Turning {} into {}", node, replacement);
							node.replace(replacement);
						}
					}
				}
			}
		});
	}
}
