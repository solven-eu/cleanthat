package eu.solven.cleanthat.rules;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IRuleDescriber;

/**
 * Switch o.equals("someString") to "someString".equals(o)
 *
 * @author Benoit Lacelle
 */
public class VariableEqualsConstant extends ATodoJavaParserRule implements IRuleDescriber {
	private static final Logger LOGGER = LoggerFactory.getLogger(VariableEqualsConstant.class);

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public boolean isPreventingExceptions() {
		return true;
	}

	@Override
	public boolean transformMethod(MethodDeclaration pre) {
		pre.walk(actualNode -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(actualNode));

			if (actualNode instanceof MethodCallExpr
					&& "equals".equals(((MethodCallExpr) actualNode).getName().getIdentifier())
					&& ((MethodCallExpr) actualNode).getArgument(0) instanceof StringLiteralExpr) {
				MethodCallExpr methodCall = (MethodCallExpr) actualNode;
				// recover argument of equals
				Expression argument = methodCall.getArgument(0);
				// hardocoded string seems to be instance of StringLiteralExpr
				LOGGER.debug("Find a hardcoded string : {}", argument);
				// argument is hard coded we need scope to inverse the two
				Optional<Expression> optScope = methodCall.getScope();
				if (optScope.isPresent()) {
					// equals must be called by something, otherwise we don't touch this part
					Expression scope = optScope.get();
					// inversion
					MethodCallExpr replacement =
							new MethodCallExpr(argument, "equals", new NodeList<Expression>(scope));
					LOGGER.info("Turning {} into {}", actualNode, replacement);
					actualNode.replace(replacement);
				}

			}
		});

		return false;
	}
}
