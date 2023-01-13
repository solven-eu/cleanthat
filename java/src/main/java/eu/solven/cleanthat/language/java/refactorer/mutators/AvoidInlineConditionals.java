package eu.solven.cleanthat.language.java.refactorer.mutators;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Turns 'boolean b = (x > 1 ) ? true : callback.doIt() || true' into 'if (x > 1) { ... } else { ...}'
 * 
 * @author Benoit Lacelle
 *
 */
public class AvoidInlineConditionals extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(AvoidInlineConditionals.class);

	private static final String METHOD_ASLIST = "asList";
	private static final String METHOD_STREAM = "stream";

	// Stream exists since 8
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Optional<String> getCheckstyleId() {
		return Optional.of("AvoidInlineConditionals");
	}

	@Override
	public String checkstyleUrl() {
		return "https://checkstyle.sourceforge.io/config_coding.html#AvoidInlineConditionals";
	}

	// TODO Lack of checking for Stream type
	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr methodCall = (MethodCallExpr) node;
		String methodCallIdentifier = methodCall.getNameAsString();
		if (!METHOD_STREAM.equals(methodCallIdentifier)) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			return false;
		}
		Expression scope = optScope.get();
		if (!(scope instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr scopeAsMethodCallExpr = (MethodCallExpr) scope;
		if (!METHOD_ASLIST.equals(scopeAsMethodCallExpr.getName().getIdentifier())) {
			return false;
		}

		Optional<Expression> optParentScope = scopeAsMethodCallExpr.getScope();
		if (optParentScope.isEmpty()) {
			return false;
		}
		Expression parentScope = optParentScope.get();
		if (!parentScope.isNameExpr()) {
			return false;
		}

		if (scopeAsMethodCallExpr.getArguments().size() != 1) {
			// TODO Handle this case with Stream.of(...)
			return false;
		}
		Expression filterPredicate = scopeAsMethodCallExpr.getArgument(0);

		boolean localTransformed = false;
		NodeList<Expression> replaceArguments = new NodeList<>(filterPredicate);
		Expression replacement = new MethodCallExpr(parentScope, METHOD_STREAM, replaceArguments);

		LOGGER.info("Turning {} into {}", methodCall, replacement);
		if (methodCall.replace(replacement)) {
			localTransformed = true;
		}

		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}
}