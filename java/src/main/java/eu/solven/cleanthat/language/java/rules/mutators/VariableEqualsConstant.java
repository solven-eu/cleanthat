package eu.solven.cleanthat.language.java.rules.mutators;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.rules.AJavaParserRule;
import eu.solven.cleanthat.language.java.rules.meta.IRuleDescriber;

/**
 * Switch o.equals("someString") to "someString".equals(o)
 *
 * @author Benoit Lacelle
 */
public class VariableEqualsConstant extends AJavaParserRule implements IRuleDescriber {

	private static final String METHOD_EQUALS = "equals";

	private static final Logger LOGGER = LoggerFactory.getLogger(VariableEqualsConstant.class);

	@Override
	public String getId() {
		return pmdId();
	}

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_bestpractices.html#literalsfirstincomparisons";
	}

	public String pmdId() {
		return "LiteralsFirstInComparisons";
	}

	@Override
	public boolean isPreventingExceptions() {
		return true;
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		MethodCallExpr methodCall = (MethodCallExpr) node;
		if (methodCall.getArguments().size() != 1) {
			return false;
		}
		Expression singleArgument = methodCall.getArgument(0);
		String methodCallName = methodCall.getName().getIdentifier();
		boolean stringScopeOnly;
		if (singleArgument instanceof ObjectCreationExpr && METHOD_EQUALS.equals(methodCallName)) {
			LOGGER.debug("This is a !String method which can be swapped");
			stringScopeOnly = false;
		} else if (singleArgument instanceof StringLiteralExpr) {
			LOGGER.debug("This is a String method which can be swapped");
			if (METHOD_EQUALS.equals(methodCallName)) {
				// We may be comparing a String with an Object
				stringScopeOnly = false;
			} else if (isSwitchableStringMethod(methodCallName)) {
				stringScopeOnly = true;
			} else {
				return false;
			}
		} else if (singleArgument instanceof FieldAccessExpr && METHOD_EQUALS.equals(methodCallName)
				&& isConstant(((FieldAccessExpr) singleArgument).getName())) {
			// TODO Think deeper about relying on the field name to suppose it is constant. We should at least check
			// the object is a class and not an object
			Expression scope = ((FieldAccessExpr) singleArgument).getScope();

			// TODO Do not switch if both the scope and the argument are constants
			// https://github.com/javaparser/javaparser/issues/3330
			// optResolvedType(scope);
			// scope.calculateResolvedType()
			LOGGER.debug("TODO Check scope is a class ({})", scope);
			LOGGER.debug("We prefer having the constant at the left");
			stringScopeOnly = true;
		} else if (singleArgument instanceof StringLiteralExpr && isCompareStringMethod(methodCallName)) {
			LOGGER.debug("TODO replace x.compareTo('bar')<0 by 'bar'.compareTo(x)>0");
			return false;
		} else {
			return false;
		}
		// recover argument of equals
		Expression argument = singleArgument;
		// hardocoded string seems to be instance of StringLiteralExpr
		LOGGER.debug("Find a hardcoded string : {}", argument);
		// argument is hard coded we need scope to inverse the two
		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			// equals must be called by something
			return false;
		}
		Expression scope = optScope.get();
		if (scope instanceof StringLiteralExpr || scope instanceof ObjectCreationExpr) {
			// There is no point in switching a constant with another constant
			return false;
		}
		if (stringScopeOnly && !isStringScope(scope)) {
			return false;
		}
		MethodCallExpr replacement = new MethodCallExpr(argument, methodCallName, new NodeList<>(scope));
		return tryReplace(node, replacement);
	}

	private static boolean isConstant(SimpleName name) {
		return name.asString().matches("[A-Z_]+");
	}

	private boolean isStringScope(Expression scope) {
		Optional<ResolvedType> optType = optResolvedType(scope);
		if (optType.isEmpty()) {
			return false;
		}
		ResolvedType type = optType.get();
		if (type.isConstraint()) {
			// This happens on lambda expression: the type we're looking for is the Lambda bound-type
			type = type.asConstraintType().getBound();
		}
		if (type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(String.class.getName())) {
			return true;
		}
		return false;
	}

	private boolean isSwitchableStringMethod(String methodCallName) {
		return METHOD_EQUALS.equals(methodCallName) || "equalsIgnoreCase".equals(methodCallName)
				|| "contentEquals".equals(methodCallName);
	}

	private boolean isCompareStringMethod(String methodCallName) {
		return "compareTo".equals(methodCallName) || "compareToIgnoreCase".equals(methodCallName);
	}
}
