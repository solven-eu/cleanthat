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
 */
// see https://jsparrow.github.io/rules/enums-without-equals.html#properties
@Deprecated(since = "Not-Ready: how can we infer a Type is an Enum?")
public class EnumsWithoutEquals extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnumsWithoutEquals.class);

	private static final ThreadLocal<JavaParserFacade> TL_JAVAPARSER = ThreadLocal.withInitial(() -> {
		CombinedTypeSolver ts = new CombinedTypeSolver();
		ts.add(new ReflectionTypeSolver());
		return JavaParserFacade.get(ts);
	});

	@Override
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
				Optional<ResolvedType> type = optResolvedType(scope);

				if (type.isPresent() && type.get().isReferenceType()) {
					LOGGER.debug("scope={} type={}", scope, type);
				}
			}
		});
		return false;
	}

}
