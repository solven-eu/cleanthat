/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;
import eu.solven.cleanthat.engine.java.refactorer.helpers.ImportDeclarationHelpers;
import eu.solven.cleanthat.engine.java.refactorer.helpers.MethodCallExprHelpers;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns 'Arrays.asList("1", 2).stream()' into 'Arrays.stream("1", 2)'
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class ArraysDotStream extends AJavaparserNodeMutator {

	private static final String METHOD_ASLIST = "asList";
	private static final String METHOD_STREAM = "stream";

	// Stream exists since 8
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("Stream");
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public Optional<String> getSonarId() {
		return Optional.of("RSPEC-3631");
	}

	@Override
	public Optional<String> getJSparrowId() {
		return Optional.of("UseArraysStream");
	}

	@Override
	public String jSparrowUrl() {
		return "https://jsparrow.github.io/rules/use-arrays-stream.html";
	}

	// TODO Lack of checking for Stream type
	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> node) {
		if (!(node.getNode() instanceof MethodCallExpr)) {
			return false;
		}
		var methodCall = (MethodCallExpr) node.getNode();
		var methodCallIdentifier = methodCall.getNameAsString();
		if (!METHOD_STREAM.equals(methodCallIdentifier)) {
			return false;
		}

		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			return false;
		}
		var scope = optScope.get();
		if (!(scope instanceof MethodCallExpr)) {
			return false;
		}
		var scopeAsMethodCallExpr = (MethodCallExpr) scope;
		if (!METHOD_ASLIST.equals(scopeAsMethodCallExpr.getName().getIdentifier())) {
			return false;
		}

		Optional<Expression> optParentScope = scopeAsMethodCallExpr.getScope();
		if (optParentScope.isEmpty()) {
			return false;
		}
		var parentScope = optParentScope.get();
		if (!parentScope.isNameExpr()) {
			return false;
		} else if (!parentScope.asNameExpr().getNameAsString().equals(Arrays.class.getSimpleName())) {
			return false;
		}

		boolean useStreamOf;

		if (scopeAsMethodCallExpr.getArguments().size() != 1) {
			useStreamOf = true;
		} else {
			var filterPredicate = scopeAsMethodCallExpr.getArgument(0);

			Optional<ResolvedType> optType = MethodCallExprHelpers.optResolvedType(node.editNode(filterPredicate));

			if (optType.isEmpty()) {
				// TODO Ask JavaParser how can one get a qualifiedname, especially given imports
				// https://github.com/javaparser/javaparser/issues/2575
				return false;
			}

			// If the input is an array (either a primitive Array, or a T[]), we rely on Arrays.stream
			useStreamOf = !optType.get().isArray();
		}

		if (useStreamOf) {
			var methodRefClassName = ImportDeclarationHelpers.getStaticMethodClassRefMayAddImport(node, Stream.class);
			var nameExpr = new NameExpr(methodRefClassName);

			// This will catch 0_argument and 2+_arguments
			// Parsing this text would produce a FieldAccessExpr instead of a NameExpr
			return tryReplace(node, new MethodCallExpr(nameExpr, "of", scopeAsMethodCallExpr.getArguments()));
		} else {
			var filterPredicate = scopeAsMethodCallExpr.getArgument(0);

			NodeList<Expression> replaceArguments = new NodeList<>(filterPredicate);
			Expression replacement = new MethodCallExpr(parentScope, METHOD_STREAM, replaceArguments);

			LOGGER.info("Turning {} into {}", methodCall, replacement);
			return tryReplace(methodCall, replacement);
		}
	}
}