package eu.solven.cleanthat.language.java.refactorer.mutators;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.AJavaParserRule;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Migrate from 's.indexOf("s")’ to ’s.indexOf('s')'.
 *
 * @author Benoit Lacelle
 */
// https://rules.sonarsource.com/java/RSPEC-1155
// https://jsparrow.github.io/rules/use-is-empty-on-collections.html
public class UseIndexOfChar extends AJavaParserRule implements IClassTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(UseIndexOfChar.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_performance.html#useindexofchar";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UseIndexOfChar");
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof StringLiteralExpr)) {
			return false;
		}
		StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
		String stringLiteralExprValue = stringLiteralExpr.getValue();
		if (stringLiteralExprValue.length() != 1) {
			// We consider only String with length == to .indexOf over the single char
			return false;
		}

		if (!stringLiteralExpr.getParentNode().isPresent()) {
			return false;
		}
		Node parentNode = stringLiteralExpr.getParentNode().get();
		if (!(parentNode instanceof MethodCallExpr)) {
			// We search a call for .indexOf
			return false;
		}
		MethodCallExpr parentMethodCall = (MethodCallExpr) parentNode;
		if (!"indexOf".equals(parentMethodCall.getNameAsString())) {
			// We search a call for .indexOf
			return false;
		}

		if (!scopeHasRequiredType(parentMethodCall.getScope(), String.class)) {
			return false;
		}

		return node.replace(new CharLiteralExpr(stringLiteralExprValue.charAt(0)));
	}
}
