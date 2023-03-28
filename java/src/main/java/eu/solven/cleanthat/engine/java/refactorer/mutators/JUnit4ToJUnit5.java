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

import static com.github.javaparser.StaticJavaParser.parseName;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserMutator;

/**
 * Migrate from JUnit4 to JUnit5/Jupiter.
 * 
 * We may better invest on OpenRewrite for a full-fledged JUnit4->JUnit5 migration
 *
 * @author Benoit Lacelle
 */
// https://junit.org/junit5/docs/current/user-guide/#migrating-from-junit4-tips
// https://jsparrow.github.io/tags/#junit
// https://www.baeldung.com/junit-5-migration
// https://docs.openrewrite.org/running-recipes/popular-recipe-guides/migrate-from-junit-4-to-junit-5
public class JUnit4ToJUnit5 extends AJavaparserMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(JUnit4ToJUnit5.class);

	// Duplicated from com.github.javaparser.ast.CompilationUnit.JAVA_LANG
	private static final String JAVA_LANG = "java.lang";

	private final Map<String, String> fromTo = ImmutableMap.<String, String>builder()
			.put("org.junit.Before", "org.junit.jupiter.api.BeforeEach")
			.put("org.junit.After", "org.junit.jupiter.api.AfterEach")
			.put("org.junit.BeforeClass", "org.junit.jupiter.api.BeforeAll")
			.put("org.junit.AfterClass", "org.junit.jupiter.api.AfterAll")
			.put("org.junit.Ignore", "org.junit.jupiter.api.Disabled")
			.put("org.junit.Assert", "org.junit.jupiter.api.Assertions")
			.put("org.junit.Test", "org.junit.jupiter.api.Test")
			.build();

	// JUnit5/Jupiter requires JDK8
	// https://junit.org/junit5/docs/current/user-guide/#overview-java-versions
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_8;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("JUnit");
	}

	@Override
	public String getId() {
		return "JUnit4ToJupiter";
	}

	@Override
	public boolean isJreOnly() {
		// JUnit classes are not JRE classes
		return false;
	}

	@Override
	public boolean walkAstHasChanged(Node tree) {
		var transformed = new AtomicBoolean(false);

		if (super.walkAstHasChanged(tree)) {
			transformed.set(true);
		}

		// Remove JUnit4 imports only after having processed the annotations, else they can not be resolved
		if (tree instanceof CompilationUnit) {
			var compilationUnit = (CompilationUnit) tree;

			// https://medium.com/@GalletVictor/migration-from-junit-4-to-junit-5-d8fe38644abe
			compilationUnit.getImports().forEach(importNode -> {
				var importName = importNode.getName().asString();
				Optional<String> optMigratedName = computeNewName(importName);

				if (optMigratedName.isPresent()) {
					importNode.setName(optMigratedName.get());
					transformed.set(true);
				}
			});
		}

		return transformed.get();
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(Node node) {
		var localTransformed = false;

		if (node instanceof AnnotationExpr) {
			localTransformed = processAnnotation((AnnotationExpr) node);
		} else if (node instanceof MethodCallExpr) {
			localTransformed = processMethodCall((MethodCallExpr) node);
		}

		if (localTransformed) {
			return true;
		} else {
			return false;
		}
	}

	private Optional<String> computeNewName(String importName) {
		Optional<String> optMigratedName;
		if ("org.junit".equals(importName)) {
			// 'org.junit.*' -> 'org.junit.jupiter.api.*'
			optMigratedName = Optional.of("org.junit.jupiter.api");
		} else {
			// 'org.junit.Test' -> 'org.junit.jupiter.api.Test'
			// String suffix = importName.substring("org.junit.".length());
			// Name adjusted = "org.junit.jupiter.api" + suffix;
			optMigratedName = Optional.ofNullable(fromTo.get(importName));
		}
		return optMigratedName;
	}

	protected boolean processAnnotation(AnnotationExpr annotation) {
		ResolvedAnnotationDeclaration resolvedAnnotation;
		try {
			// https://github.com/javaparser/javaparser/issues/1621
			resolvedAnnotation = annotation.resolve();
		} catch (UnsolvedSymbolException e) {
			// This typically happens when processing a node after having migrated to junit5: junit5 annotations are
			// unknown, hence this fails
			LOGGER.debug("We were not able to resolve annotation: {}", annotation);
			return false;
		}
		var qualifiedName = resolvedAnnotation.getQualifiedName();

		Optional<String> optMigratedName = computeNewName(qualifiedName);

		var localTransformed = false;
		if (optMigratedName.isPresent()) {
			var migratedName = optMigratedName.get();

			var currentName = annotation.getNameAsString();
			String newName;

			if (currentName.indexOf('.') >= 0) {
				// The existing name is fully qualified
				newName = migratedName;
			} else {
				// The existing name is the simple className
				var lastDot = migratedName.lastIndexOf('.');
				if (lastDot < 0) {
					// No dot: the class is in the root package
					newName = migratedName;
				} else {
					newName = migratedName.substring(lastDot + 1);
				}
			}

			if (!currentName.equals(newName)) {
				localTransformed = true;
				annotation.setName(migratedName);
			}
		}
		return localTransformed;
	}

	private boolean processMethodCall(MethodCallExpr methodCall) {
		Optional<Expression> optScope = methodCall.getScope();
		if (optScope.isEmpty()) {
			// TODO Document when this would happen
			return false;
		}
		var scope = optScope.get();
		Optional<ResolvedType> type = optResolvedType(scope);

		if (type.isPresent() && type.get().isReferenceType()) {
			var referenceType = type.get().asReferenceType();

			var oldQualifiedName = referenceType.getQualifiedName();
			Optional<String> optNewName = computeNewName(oldQualifiedName);
			if (optNewName.isPresent()) {
				var newQualifiedName = optNewName.get();

				// JUnit5 imports are not added yet: check import status based on JUnit4 imports
				// see com.github.javaparser.ast.CompilationUnit.addImport(ImportDeclaration)
				var compilationUnit = methodCall.findAncestor(CompilationUnit.class).get();
				var imported = isImported(compilationUnit, new ImportDeclaration(newQualifiedName, false, false));

				NameExpr newScope;
				if (imported) {
					var newSimpleName = newQualifiedName.substring(newQualifiedName.lastIndexOf('.') + 1);
					newScope = new NameExpr(newSimpleName);
				} else {
					newScope = new NameExpr(newQualifiedName);
				}
				var replacement = new MethodCallExpr(newScope, methodCall.getNameAsString(), methodCall.getArguments());
				return methodCall.replace(replacement);
			}
		}
		return false;
	}

	// Mostly duplicated from com.github.javaparser.ast.CompilationUnit.addImport(ImportDeclaration)
	private boolean isImported(CompilationUnit compilationUnit, ImportDeclaration importDeclaration) {
		return !isImplicitImport(compilationUnit, importDeclaration) && compilationUnit.getImports()
				.stream()
				.noneMatch(im -> im.equals(importDeclaration) || im.isAsterisk() && Objects
						.equals(getImportPackageName(im).get(), getImportPackageName(importDeclaration).orElse(null)));
	}

	/**
	 * @param importDeclaration
	 * @return {@code true}, if the import is implicit
	 */
	// Duplicated from com.github.javaparser.ast.CompilationUnit.isImplicitImport(ImportDeclaration)
	private boolean isImplicitImport(CompilationUnit compilationUnit, ImportDeclaration importDeclaration) {
		Optional<Name> importPackageName = getImportPackageName(importDeclaration);
		if (importPackageName.isPresent()) {
			if (parseName(JAVA_LANG).equals(importPackageName.get())) {
				// java.lang is implicitly imported
				return true;
			}
			if (compilationUnit.getPackageDeclaration().isPresent()) {
				// the import is within the same package
				var currentPackageName = compilationUnit.getPackageDeclaration().get().getName();
				return currentPackageName.equals(importPackageName.get());
			}
			return false;
		} else {
			// imports of unnamed package are not allowed
			return true;
		}
	}

	// Duplicated from com.github.javaparser.ast.CompilationUnit.getImportPackageName(ImportDeclaration)
	@SuppressWarnings("checkstyle:AvoidInlineConditionals")
	private static Optional<Name> getImportPackageName(ImportDeclaration importDeclaration) {
		return (importDeclaration.isAsterisk() ? new Name(importDeclaration.getName(), "*")
				: importDeclaration.getName()).getQualifier();
	}
}
