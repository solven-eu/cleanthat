package eu.solven.cleanthat.rules;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * Prevent relying .equals on {@link Enum} types
 * 
 * @author Benoit Lacelle
 *
 */
// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
@Deprecated(since = "Not-Ready: how can we infer a Type is an Enum?")
public class EnumsWithoutEquals implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(EnumsWithoutEquals.class);

	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_5;
	}

	@Override
	public boolean transform(MethodDeclaration pre) {
		// https://stackoverflow.com/questions/55309460/how-to-replace-expression-by-string-in-javaparser-ast
		pre.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));

			if (node instanceof MethodCallExpr && "equals".equals(((MethodCallExpr) node).getName().getIdentifier())) {
				MethodCallExpr methodCall = (MethodCallExpr) node;
				Optional<Expression> optScope = methodCall.getScope();
				if (!optScope.isPresent()) {
					// TODO Document when this would happen
					return;
				}
				Expression scope = optScope.get();

				CombinedTypeSolver ts = new CombinedTypeSolver();
				ts.add(new ReflectionTypeSolver());

				ResolvedType type;
				try {
					type = JavaParserFacade.get(ts).getType(scope);
				} catch (RuntimeException e) {
					// UnsolvedSymbolException
					// https://github.com/javaparser/javaparser/issues/1491
					LOGGER.debug("What are we doing wrong here?", e);
					LOGGER.warn("Issue with JavaParser: {} {}", e.getClass().getName(), e.getMessage());
					return;
				}

				if (type.isReferenceType()) {
					LOGGER.debug("scope={} type={}", scope, type);
				}
			}
		});

		return false;
	}
}
