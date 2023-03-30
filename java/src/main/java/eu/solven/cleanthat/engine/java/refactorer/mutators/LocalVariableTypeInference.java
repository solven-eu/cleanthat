/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.Pair;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns 'int i = 10;' into 'var i = 10'
 *
 * @author Benoit Lacelle
 */
// https://github.com/openrewrite/rewrite/issues/1656
// https://stackoverflow.com/questions/49210591/restrictions-on-using-local-variable-type-inference-in-java-10
// https://openjdk.org/projects/amber/guides/lvti-style-guide
public class LocalVariableTypeInference extends AJavaparserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalVariableTypeInference.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_10;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("ExplicitToImplicit");
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-6212");
	}

	@Override
	public Set<String> getSeeUrls() {
		return Set.of("https://openjdk.org/jeps/286");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("LocalVariableTypeInference");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/local-variable-type-inference.html";
	}

	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof VariableDeclarationExpr)) {
			return false;
		}
		var variableDeclarationExpr = (VariableDeclarationExpr) node;

		if (variableDeclarationExpr.getVariables().size() >= 2) {
			return false;
		}

		var singleVariableDeclaration = variableDeclarationExpr.getVariable(0);

		var type = singleVariableDeclaration.getType();
		if (type.isVarType()) {
			return false;
		}

		// https://github.com/javaparser/javaparser/issues/3898
		// We can not change the Type, as it would fail in the case of Type with Diamond
		var initializer = singleVariableDeclaration.getInitializer().orElse(null);

		if (!isReplaceableAssignement(type, initializer)) {
			return false;
		}

		var newVariableDeclarator =
				new VariableDeclarator(new VarType(), singleVariableDeclaration.getName(), initializer);
		// We can not change the VariableDeclarator, as it would fail in the case of Type with Diamond
		// return singleVariableDeclaration.replace(newVariableDeclarator);

		var newVariableDeclarationExpr = new VariableDeclarationExpr(newVariableDeclarator);

		newVariableDeclarationExpr.setModifiers(variableDeclarationExpr.getModifiers());
		newVariableDeclarationExpr.setAnnotations(variableDeclarationExpr.getAnnotations());

		return variableDeclarationExpr.replace(newVariableDeclarationExpr);
	}

	private boolean isReplaceableAssignement(Type variableType, Expression initializer) {
		if (initializer == null) {
			return false;
		} else if (initializer.isLambdaExpr()) {
			// LambdaExpr can not be in a `var`
			return false;
		}

		Optional<ResolvedType> optInitializerType = optResolvedType(initializer);
		if (optInitializerType.isEmpty()) {
			return false;
		}
		// TODO Would there be a way to get the mostly qualified type (i.e. based on imports, and no package if the type
		// was actually coming from a wildcard import)
		Optional<ResolvedType> optVariableType = optResolvedType(variableType);
		if (optVariableType.isEmpty()) {
			return false;
		}

		// If the variable was List but allocating an ArrayList, it means we can assign later any List
		// `var` would forbid assigning anything but an ArrayList
		var initializerType = optInitializerType.get();
		var resolvedVariableType = optVariableType.get();
		if (!initializerType.describe().equals(resolvedVariableType.describe())) {
			return false;
		}

		if (initializerType.isReferenceType() && resolvedVariableType.isReferenceType()) {
			// We require generics to be the same in both sides
			List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> variableTypeParametersMap;
			try {
				var variableReferenceType = resolvedVariableType.asReferenceType();
				variableTypeParametersMap = variableReferenceType.getTypeParametersMap();
			} catch (UnsolvedSymbolException e) {
				LOGGER.debug("Issue solving a Symbol type: {}", e);
				return false;
			}

			List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> initializerTypeParametersMap;
			try {
				var initializerReferenceType = initializerType.asReferenceType();
				initializerTypeParametersMap = initializerReferenceType.getTypeParametersMap();
			} catch (UnsolvedSymbolException e) {
				LOGGER.debug("Issue solving a Symbol type: {}", e);
				return false;
			}

			// In fact, it is not enough. See
			// eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.LocalVariableTypeInferenceCases.Case_UnclearGenericBounds
			// return initializerTypeParametersMap.equals(variableTypeParametersMap);

			return initializerTypeParametersMap.isEmpty() && variableTypeParametersMap.isEmpty();
		}

		return true;
	}
}
