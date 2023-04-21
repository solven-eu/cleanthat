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
package eu.solven.cleanthat.engine.java.refactorer.helpers;

import static com.github.javaparser.StaticJavaParser.parseName;

import java.util.Objects;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;

import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Helps working with {@link ImportDeclaration}
 * 
 * @author Benoit Lacelle
 *
 */
public class ImportDeclarationHelpers {
	// Duplicated from com.github.javaparser.ast.CompilationUnit.JAVA_LANG
	private static final String JAVA_LANG = "java.lang";

	protected ImportDeclarationHelpers() {
		// hidden
	}

	/**
	 * 
	 * @param compilationUnit
	 * @param methodRefPackage
	 * @param qualifiedName
	 * @return true if the given qualifiedName (which may be a nested Class) in given package is already imported in
	 *         given CompilationUnit
	 */
	public static boolean isImported(NodeAndSymbolSolver<?> compilationUnit,
			String methodRefPackage,
			String qualifiedName) {
		Optional<PackageDeclaration> optPackageDeclaration = compilationUnit.getPackageDeclaration();
		if (optPackageDeclaration.isPresent()) {
			var packageDecl = optPackageDeclaration.get().getNameAsString();

			// see UnnecessaryImport.removeSamePackageImports(Collection<ImportDeclaration>,
			// Optional<PackageDeclaration>)
			if (methodRefPackage.equals(packageDecl)) {
				return true;
			}
		}

		NodeList<ImportDeclaration> imports = compilationUnit.getImports();

		if (imports.isEmpty() && methodRefPackage.indexOf('.') >= 0) {
			return false;
		}

		if (JAVA_LANG.equals(methodRefPackage)) {
			return true;
		}
		// TODO manage wildcards/asterisks
		return imports.stream().anyMatch(id -> id.getNameAsString().equals(qualifiedName));
	}

	// https://stackoverflow.com/questions/147454/why-is-using-a-wild-card-with-a-java-import-statement-bad
	public static String getStaticMethodClassRefMayAddImport(NodeAndSymbolSolver<?> node, Class<?> clazz) {
		var qualifiedName = clazz.getName();

		var classSimpleName = clazz.getSimpleName();

		String methodRefClassName;
		if (isImported(node, clazz.getPackageName(), qualifiedName)) {
			methodRefClassName = classSimpleName;
		} else if (isImportable(node, qualifiedName)) {
			node.addImport(qualifiedName, false, false);
			methodRefClassName = classSimpleName;
		} else {
			methodRefClassName = qualifiedName;

		}
		return methodRefClassName;
	}

	protected static String getSimpleName(String qualifiedName) {
		var indexOfDot = qualifiedName.lastIndexOf('.');

		if (indexOfDot < 0) {
			return qualifiedName;
		} else {
			return qualifiedName.substring(indexOfDot + 1);
		}
	}

	/**
	 * 
	 * @param context
	 * @param qualifiedName
	 * @return true if the given qualifiedName (which may be a nested Class) in given package can be imported in given
	 *         CompilationUnit without conflicting existing imports
	 */
	public static boolean isImportable(NodeAndSymbolSolver<?> context, String qualifiedName) {
		NodeList<ImportDeclaration> imports = context.getImports();

		var tokenName = getSimpleName(qualifiedName);

		// There is already a wildcard import: it may hold the token name
		return imports.stream()
				.noneMatch(id -> id.isAsterisk() || getSimpleName(id.getNameAsString()).equals(tokenName));
	}

	public static NameExpr nameOrQualifiedName(NodeAndSymbolSolver<?> compilationUnit, Class<?> clazz) {
		return new NameExpr(nameOrQualifiedNameAsString(compilationUnit, clazz));
	}

	private static String nameOrQualifiedNameAsString(NodeAndSymbolSolver<?> compilationUnit, Class<?> clazz) {
		if (isImported(compilationUnit, clazz.getPackageName(), clazz.getName())) {
			return clazz.getSimpleName();
		} else {
			return clazz.getName();
		}
	}

	public static boolean isImported(NodeAndSymbolSolver<? extends Expression> expr, String imported) {
		Optional<CompilationUnit> optCompilationUnit = expr.getNode().findCompilationUnit();
		if (optCompilationUnit.isEmpty()) {
			return false;
		}

		return optCompilationUnit.get()
				.getImports()
				.stream()
				.anyMatch(importDecl -> !importDecl.isAsterisk() && !importDecl.isStatic()
						&& imported.equals(importDecl.getNameAsString()));
	}

	// Mostly duplicated from com.github.javaparser.ast.CompilationUnit.addImport(ImportDeclaration)
	public static boolean isImported(NodeAndSymbolSolver<? extends Expression> context,
			ImportDeclaration importDeclaration) {
		return !isImplicitImport(context, importDeclaration) && context.getImports()
				.stream()
				.noneMatch(im -> im.equals(importDeclaration) || im.isAsterisk() && Objects
						.equals(getImportPackageName(im).get(), getImportPackageName(importDeclaration).orElse(null)));
	}

	/**
	 * @param importDeclaration
	 * @return {@code true}, if the import is implicit
	 */
	// Duplicated from com.github.javaparser.ast.CompilationUnit.isImplicitImport(ImportDeclaration)
	private static boolean isImplicitImport(NodeAndSymbolSolver<? extends Expression> context,
			ImportDeclaration importDeclaration) {
		Optional<Name> importPackageName = getImportPackageName(importDeclaration);
		if (importPackageName.isPresent()) {
			if (parseName(JAVA_LANG).equals(importPackageName.get())) {
				// java.lang is implicitly imported
				return true;
			}
			if (context.getPackageDeclaration().isPresent()) {
				// the import is within the same package
				var currentPackageName = context.getPackageDeclaration().get().getName();
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
