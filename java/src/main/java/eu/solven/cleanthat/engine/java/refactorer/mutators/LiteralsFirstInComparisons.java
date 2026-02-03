/*
 * Copyright 2023-2026 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.engine.java.refactorer.mutators;

import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserExprMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import eu.solven.cleanthat.engine.java.refactorer.meta.ApplyBeforeMe;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutatorDescriber;
import lombok.extern.slf4j.Slf4j;

/**
 * Switch o.equals("someString") to "someString".equals(o)
 *
 * @author Benoit Lacelle
 */
@SuppressWarnings("PMD.GodClass")
@Slf4j
@ApplyBeforeMe(UnnecessaryCaseChange.class)
public class LiteralsFirstInComparisons extends AJavaparserExprMutator implements IMutatorDescriber {

	private static final String METHOD_COMPARE_TO = "compareTo";
	private static final String METHOD_COMPARE_TO_IGNORE_CASE = "compareToIgnoreCase";
	private static final String METHOD_CONTENT_EQUALS = "contentEquals";
	private static final String METHOD_EQUALS = "equals";
	private static final String METHOD_EQUALS_IGNORE_CASE = "equalsIgnoreCase";

	private static final Set<String> SWITCHABLE_STRING_METHODS =
			Set.of(METHOD_EQUALS, METHOD_EQUALS_IGNORE_CASE, METHOD_CONTENT_EQUALS);

	private static final Set<String> COMPARE_STRING_METHODS = Set.of(METHOD_COMPARE_TO, METHOD_COMPARE_TO_IGNORE_CASE);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("MayPreventException");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/pmd/pmd_rules_java_bestpractices.html#literalsfirstincomparisons";
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("LiteralsFirstInComparisons");
	}

	/**
	 * {@link LiteralsFirstInComparisons} may turn NullPointerException into false.
	 */
	@Override
	public boolean isPreventingExceptions() {
		return true;
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processExpression(NodeAndSymbolSolver<Expression> node) {
		if (!(node.getNode() instanceof MethodCallExpr)) {
			return false;
		}
		var methodCall = (MethodCallExpr) node.getNode();
		if (methodCall.getArguments().size() != 1) {
			return false;
		}
		var singleArgument = methodCall.getArgument(0);
		var methodCallName = methodCall.getName().getIdentifier();
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
		} else if (METHOD_EQUALS.equals(methodCallName)
				&& (singleArgument instanceof FieldAccessExpr || singleArgument instanceof NameExpr)
		// && isConstant(((NodeWithSimpleName<?>) singleArgument).getName())
		) {
			// We may switch if the scope is a variable
			stringScopeOnly = false;
		} else if (singleArgument instanceof StringLiteralExpr && isCompareStringMethod(methodCallName)) {
			LOGGER.debug("TODO replace x.compareTo('bar')<0 by 'bar'.compareTo(x)>0");
			return false;
		} else {
			return false;
		}

		// recover argument of equals
		var argument = singleArgument;
		// hardcoded string seems to be instance of StringLiteralExpr
		LOGGER.debug("Find a hardcoded string : {}", argument);

		// argument is hard coded we need scope to inverse the two
		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			// equals must be called by something
			return false;
		}
		var scope = optScope.get();

		if (stringScopeOnly && !MethodCallExprHelpers.scopeHasRequiredType(node.editNode(scope), String.class)) {
			return false;
		}

		if (!mayBeNull(scope)) {
			// The scope can not be null: preserve the current style
			return false;
		} else if (!mayBeNull(argument)
				// Static fields are considered notNull when compared with a notStatic thing
				|| isStaticField(argument) && !isStaticField(scope)) {
			var replacement = new MethodCallExpr(argument, methodCallName, new NodeList<>(scope));
			return tryReplace(node, replacement);
		} else {
			// There is no point in switching a constant with another constant
			// Or switching a nullable with another nullable
			return false;
		}
	}

	private boolean mayBeNull(Expression expr) {
		if (expr instanceof StringLiteralExpr || expr instanceof ObjectCreationExpr
				|| expr instanceof SuperExpr
				|| expr instanceof ThisExpr) {
			return false;
		}
		return true;
	}

	private boolean isStaticField(Expression singleArgument) {
		boolean argumentIsField;
		if (singleArgument instanceof NameExpr || singleArgument instanceof FieldAccessExpr) {
			Optional<ResolvedDeclaration> optResolved = optResolved(singleArgument);

			if (optResolved.isEmpty()) {
				return looksLikeAConstant(((NodeWithSimpleName<?>) singleArgument).getName());
			} else {
				var resolved = optResolved.get();

				if (resolved.isField() && resolved.asField().isStatic()) {
					argumentIsField = true;
				} else {
					argumentIsField = false;
				}
			}
		} else {
			argumentIsField = false;
		}
		return argumentIsField;
	}

	private static boolean looksLikeAConstant(SimpleName name) {
		return name.asString().matches("[A-Z0-9_]+");
	}

	private boolean isSwitchableStringMethod(String methodCallName) {
		return SWITCHABLE_STRING_METHODS.contains(methodCallName);
	}

	private boolean isCompareStringMethod(String methodCallName) {
		return COMPARE_STRING_METHODS.contains(methodCallName);
	}
}
