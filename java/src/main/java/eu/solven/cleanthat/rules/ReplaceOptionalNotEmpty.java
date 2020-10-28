package eu.solven.cleanthat.rules;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
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

			if (node instanceof MethodCallExpr && "equals".equals(((MethodCallExpr) node).getName().getIdentifier())) {
				MethodCallExpr methodCall = (MethodCallExpr) node;
				//recover argument of equals
				Expression argument = methodCall.getArgument(0);
				//hardocoded string seems to be instance of StringLiteralExpr
				if(argument instanceof StringLiteralExpr) {
					LOGGER.debug("Find a hardcoded string : {}",argument);
					//argument is hard coded <e need scope to inverse the two
					Optional<Expression> optScope = methodCall.getScope();
					if (!optScope.isPresent()) {
						// TODO Document when this would happen
						return;
					}
					Expression scope = optScope.get();
					//inversion
					MethodCallExpr replacement =
							new MethodCallExpr(argument, "equals", new NodeList<Expression>(scope));
					LOGGER.info("Turning {} into {}", node, replacement);
					node.replace(replacement);
				}
			}
		});

		return false;
	}
}