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

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.AJavaParserMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.pepper.logging.PepperLogHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 *
 * @author Benoit Lacelle
 */
public class UnnecessaryFullyQualifiedName extends AJavaParserMutator implements IMutator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UnnecessaryFullyQualifiedName.class);

	// Optional exists since 8
	// Optional.isPresent exists since 11
	@Override
	public String minimalJavaVersion() {
		return IJdkVersionConstants.JDK_11;
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
	protected boolean processNotRecursively(Node node) {
		LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));

		if (!(node instanceof NodeWithType)) {
			return false;

			// We can improve this by handling statis method calls
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

		boolean doSimplifyType;

		// https://stackoverflow.com/questions/51257256/why-dont-we-import-java-lang-package
		if (nodeType.asString().startsWith("java.lang")) {
			doSimplifyType = true;
		} else {
			List<ImportDeclaration> imports = getImports(node);

			Optional<ImportDeclaration> optMatchingImport = searchMatchingImport(imports, nodeWithType.getType());
			if (optMatchingImport.isEmpty()) {
				doSimplifyType = false;
			} else {
				doSimplifyType = true;
			}
		}

		if (!doSimplifyType) {
			return false;
		}

		int nodeTypeLastDot = nodeType.asString().lastIndexOf('.');
		if (nodeTypeLastDot < 0) {
			LOGGER.debug("Import without a '.' ?");
			return false;
		}

		String newType = nodeWithType.getType().asString().substring(nodeTypeLastDot + 1);
		nodeWithType.setType(newType);

		return true;
	}

	private List<ImportDeclaration> getImports(Node node) {
		Node root = node;
		while (root.getParentNode().isPresent()) {
			root = root.getParentNode().get();
		}

		ImportVisitorAdapter visitor = new ImportVisitorAdapter();
		root.accept(visitor, null);

		List<ImportDeclaration> imports = visitor.getImports();
		return imports;
	}

	private Optional<ImportDeclaration> searchMatchingImport(List<ImportDeclaration> imports, Type type) {
		return imports.stream().filter(i -> {
			String importedTypeOrPackage = i.getNameAsString();
			String nodeTypeMayDiamond = type.asString();

			if (nodeTypeMayDiamond.indexOf('<') >= 0) {
				nodeTypeMayDiamond = nodeTypeMayDiamond.substring(0, nodeTypeMayDiamond.indexOf('<'));
			}

			return importedTypeOrPackage.equals(nodeTypeMayDiamond);
		}).findFirst();
	}
}
