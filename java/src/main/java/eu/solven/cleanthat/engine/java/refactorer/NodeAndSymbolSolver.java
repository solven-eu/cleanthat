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
package eu.solven.cleanthat.engine.java.refactorer;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;

import lombok.Data;

/**
 * As we may process cloned {@link Node}, and such {@link Node} has no {@link CompilationUnit}, we need to transport the
 * {@link SymbolSolver} manually.
 * 
 * @author Benoit Lacelle
 *
 */
@Data
public class NodeAndSymbolSolver<T extends Node> {
	final T node;
	final SymbolResolver symbolResolver;

	final CompilationUnit compilationUnit;
	final Optional<PackageDeclaration> packageDeclaration;
	final NodeList<ImportDeclaration> imports;

	public <S extends Node> NodeAndSymbolSolver<S> editNode(S s) {
		return new NodeAndSymbolSolver<>(s, symbolResolver, compilationUnit, packageDeclaration, imports);
	}

	public void addImport(String qualifiedName, boolean isStatic, boolean isAsterisk) {
		compilationUnit.addImport(qualifiedName, isStatic, isAsterisk);
	}

	public static NodeAndSymbolSolver<?> make(Node node) {
		CompilationUnit compilationUnit = node.findCompilationUnit().get();
		return new NodeAndSymbolSolver<>(node,
				node.getSymbolResolver(),
				compilationUnit,
				compilationUnit.getPackageDeclaration(),
				compilationUnit.getImports());
	}

	public Optional<NodeAndSymbolSolver<? extends Expression>> editNode(Optional<Expression> scope) {
		return scope.map(this::editNode);
	}
}
