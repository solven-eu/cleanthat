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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Turns 'Arrays.asList("1", 2).stream()' into 'Arrays.stream("1", 2)'
 * 
 * @author Benoit Lacelle
 *
 */
public class ArraysDotStream extends AJavaparserMutator {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamAnyMatch.class);

	private static final String METHOD_ASLIST = "asList";
	private static final String METHOD_STREAM = "stream";

	// Stream exists since 8
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public boolean isDraft() {
		return IS_PRODUCTION_READY;
	}

	@Override
	public String getId() {
		return "ArraysDotStream";
	}

	@Override
	public String jsparrowUrl() {
		return "https://jsparrow.github.io/rules/use-arrays-stream.html";
	}

	// TODO Lack of checking for Stream type
	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		if (!(node instanceof MethodCallExpr)) {
			return false;
		}
		var methodCall = (MethodCallExpr) node;
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

			Optional<ResolvedType> optType = optResolvedType(filterPredicate);

			if (optType.isEmpty()) {
				// TODO Ask JavaParser how can one get a qualifiedname, especially given imports
				// https://github.com/javaparser/javaparser/issues/2575
				return false;
			}

			// If the input is an array (either a primitive Array, or a T[]), we rely on Arrays.stream
			useStreamOf = !optType.get().isArray();
		}

		if (useStreamOf) {
			Optional<CompilationUnit> compilationUnit = node.findAncestor(CompilationUnit.class);
			var methodRefClassName = getStaticMethodClassRefMayAddImport(compilationUnit, Stream.class);
			var nameExpr = new NameExpr(methodRefClassName);

			// This will catch 0_argument and 2+_arguments
			// Parsing this text would produce a FieldAccessExpr instead of a NameExpr
			return node.replace(new MethodCallExpr(nameExpr, "of", scopeAsMethodCallExpr.getArguments()));
		} else {
			var filterPredicate = scopeAsMethodCallExpr.getArgument(0);

			NodeList<Expression> replaceArguments = new NodeList<>(filterPredicate);
			Expression replacement = new MethodCallExpr(parentScope, METHOD_STREAM, replaceArguments);

			LOGGER.info("Turning {} into {}", methodCall, replacement);
			return methodCall.replace(replacement);
		}
	}

	// https://stackoverflow.com/questions/147454/why-is-using-a-wild-card-with-a-java-import-statement-bad
	private String getStaticMethodClassRefMayAddImport(Optional<CompilationUnit> optCompilationUnit, Class<?> clazz) {
		var qualifiedName = clazz.getName();
		if (optCompilationUnit.isEmpty()) {
			return qualifiedName;
		}

		var classSimpleName = clazz.getSimpleName();

		String methodRefClassName;
		var compilationUnit = optCompilationUnit.get();
		if (isImported(compilationUnit, clazz.getPackageName(), qualifiedName)) {
			methodRefClassName = classSimpleName;
		} else if (isImportable(compilationUnit, qualifiedName)) {
			compilationUnit.addImport(qualifiedName, false, false);
			methodRefClassName = classSimpleName;
		} else {
			methodRefClassName = qualifiedName;

		}
		return methodRefClassName;
	}
}