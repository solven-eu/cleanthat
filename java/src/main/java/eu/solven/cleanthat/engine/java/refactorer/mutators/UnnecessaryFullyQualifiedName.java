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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.ImmutableSet;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaparserNodeMutator;
import eu.solven.cleanthat.engine.java.refactorer.NodeAndSymbolSolver;

/**
 * Turns 'java.lang.String' into 'String'
 *
 * @author Benoit Lacelle
 */
public class UnnecessaryFullyQualifiedName extends AJavaparserNodeMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UnnecessaryFullyQualifiedName.class);

	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_1;
	}

	@Override
	public Set<String> getTags() {
		return ImmutableSet.of("ExplicitToImplicit");
	}

	@Override
	public Optional<String> getPmdId() {
		return Optional.of("UnnecessaryFullyQualifiedName");
	}

	@Override
	public String pmdUrl() {
		return "https://pmd.github.io/latest/pmd_rules_java_codestyle.html#unnecessaryfullyqualifiedname";
	}

	/**
	 * Collects the {@link ImportDeclaration} associated to a {@link Node} into a {@link List}
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	public static class ImportVisitorAdapter extends VoidVisitorAdapter<Object> {

		final List<ImportDeclaration> imports = new ArrayList<>();

		@Override
		public void visit(ImportDeclaration n, Object arg) {
			super.visit(n, arg);

			imports.add(n);
		}

		public List<ImportDeclaration> getImports() {
			return imports;
		}
	}

	@SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NPathComplexity" })
	@Override
	protected boolean processNotRecursively(NodeAndSymbolSolver<?> nodeAndContext) {
		Node node = nodeAndContext.getNode();
		if (!(node instanceof NodeWithType)) {
			return false;

			// We can improve this by handling static method calls
			// if (!(node instanceof MethodCallExpr)) {
			// return false;
			// }
			//
			// MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			//
			// Optional<Expression> scope = methodCallExpr.getScope();
			// if (scope.isPresent() && scope.get().isFieldAccessExpr()) {
			// FieldAccessExpr fae = scope.get().asFieldAccessExpr();
			//
			// fae.getName().getNameAsString()
			// fae.getTypeArguments();
			// }

		}

		NodeWithType<?, ?> nodeWithType = (NodeWithType<?, ?>) node;
		Type nodeType = nodeWithType.getType();

		// https://stackoverflow.com/questions/51257256/why-dont-we-import-java-lang-package
		Optional<String> optImportedPrefix;
		if (nodeType.asString().startsWith("java.lang")) {
			optImportedPrefix = Optional.of("java.lang.*");
		} else {
			List<ImportDeclaration> imports = getImports(node);

			Optional<ImportDeclaration> optMatchingImport = searchMatchingImport(imports, nodeWithType.getType());

			optImportedPrefix = optMatchingImport.map(id -> withAsterisk(id));
		}

		if (optImportedPrefix.isEmpty()) {
			return false;
		}

		var nodeTypeLastDot = optImportedPrefix.get().lastIndexOf('.');
		if (nodeTypeLastDot < 0) {
			LOGGER.debug("Import without a '.' ?: {}", optImportedPrefix);
			return false;
		}

		// `+1` to skip the dot
		String newType = nodeWithType.getType().asString().substring(nodeTypeLastDot + 1);
		nodeWithType.setType(newType);

		return true;
	}

	private String withAsterisk(ImportDeclaration id) {
		String importedName = id.getNameAsString();
		if (id.isAsterisk()) {
			return importedName + ".*";
		} else {
			return importedName;
		}
	}

	private List<ImportDeclaration> getImports(Node node) {
		var root = node;
		while (root.getParentNode().isPresent()) {
			root = root.getParentNode().get();
		}

		var visitor = new ImportVisitorAdapter();
		root.accept(visitor, null);

		List<ImportDeclaration> imports = visitor.getImports();
		return imports;
	}

	private Optional<ImportDeclaration> searchMatchingImport(List<ImportDeclaration> imports, Type type) {
		return imports.stream().filter(i -> {
			var importedTypeOrPackage = i.getNameAsString();
			var nodeTypeMayDiamond = type.asString();

			String nodeTypeNoDiamond;
			if (nodeTypeMayDiamond.indexOf('<') >= 0) {
				nodeTypeNoDiamond = nodeTypeMayDiamond.substring(0, nodeTypeMayDiamond.indexOf('<'));
			} else {
				nodeTypeNoDiamond = nodeTypeMayDiamond;
			}

			if (nodeTypeNoDiamond.equals(importedTypeOrPackage)) {
				// this is an exact match
				return true;
			} else if (nodeTypeNoDiamond.startsWith(importedTypeOrPackage + ".")) {
				// this is an exact match
				return true;
			} else {
				return false;
			}
		}).findFirst();
	}
}
