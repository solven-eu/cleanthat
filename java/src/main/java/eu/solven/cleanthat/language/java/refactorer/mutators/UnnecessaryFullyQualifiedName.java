package eu.solven.cleanthat.language.java.refactorer.mutators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import eu.solven.cleanthat.language.java.IJdkVersionConstants;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.AJavaParserRule;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Prefer 'o.isPresent()' over 'o.isEmpty() == 0'
 *
 * @author Benoit Lacelle
 */
public class UnnecessaryFullyQualifiedName extends AJavaParserRule implements IClassTransformer {
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
			if (!optMatchingImport.isPresent()) {
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
