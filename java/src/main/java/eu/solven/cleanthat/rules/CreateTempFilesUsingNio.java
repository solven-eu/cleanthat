package eu.solven.cleanthat.rules;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * cases inspired from https://jsparrow.github.io/rules/create-temp-files-using-java-nio.html#description
 *
 * @author Sébastien Collard
 */

public class CreateTempFilesUsingNio extends AJavaParserRule implements IClassTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(UseIsEmptyOnCollections.class);

	@Override
	public String minimalJavaVersion() {
		// java.nio.Files has been introduced in JDK7
		return IJdkVersionConstants.JDK_7;
	}

	@Override
	public boolean transform(MethodDeclaration pre) {
		AtomicBoolean hasTransformed = new AtomicBoolean();
		pre.walk(node -> {
			LOGGER.debug("{}", PepperLogHelper.getObjectAndClass(node));
			// ResolvedMethodDeclaration test;
			if (!(node instanceof MethodCallExpr)) {
				return;
			}

			MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			if (!"createTempFile".equals(methodCallExpr.getName().getIdentifier())) {
				return;
			}

			Optional<Boolean> optIsStatic = mayStaticCall(methodCallExpr);

			if (optIsStatic.isPresent() && optIsStatic.get() == Boolean.FALSE) {
				return;
			}

			Optional<Expression> optScope = methodCallExpr.getScope();

			if (optScope.isPresent()) {
				Optional<ResolvedType> type = optResolvedType(optScope.get());

				if (!type.isPresent() || !"java.io.File".equals(type.get().asReferenceType().getQualifiedName())) {
					return;
				}

				LOGGER.debug("Trouvé : {}", node.toString());
				if (process(methodCallExpr)) {
					hasTransformed.set(true);
				}
			}
		});
		return hasTransformed.get();

	}

	private Optional<Boolean> mayStaticCall(MethodCallExpr methodCallExpr) {
		try {
			return Optional.of(methodCallExpr.resolve().isStatic());
		} catch (Exception e) {
			// Knowing if class is static requires a SymbolResolver:
			// 'java.lang.IllegalStateException: Symbol resolution not configured: to configure consider setting a
			// SymbolResolver in the ParserConfiguration'
			LOGGER.debug("arg", e);

			return Optional.empty();
		}
	}

	private boolean process(MethodCallExpr methodExp) {
		List<Expression> arguments = methodExp.getArguments();

		Optional<MethodCallExpr> optToPath;

		NameExpr newStaticClass = new NameExpr("Files");
		String newStaticMethod = "createTempFile";
		int minArgSize = 2;
		if (arguments.size() == minArgSize) {
			// Create in default tmp directory
			LOGGER.debug("Add java.nio.file.Files to import");
			methodExp.tryAddImportToParentCompilationUnit(Files.class);

			optToPath = Optional.of(new MethodCallExpr(newStaticClass, newStaticMethod, methodExp.getArguments()));
		} else if (arguments.size() == minArgSize + 1) {
			Expression arg0 = methodExp.getArgument(0);
			Expression arg1 = methodExp.getArgument(1);
			Expression arg3 = methodExp.getArgument(2);
			if (arg3.isObjectCreationExpr()) {
				methodExp.tryAddImportToParentCompilationUnit(Paths.class);

				ObjectCreationExpr objectCreation = (ObjectCreationExpr) methodExp.getArgument(minArgSize);
				NodeList<Expression> objectCreationArguments = objectCreation.getArguments();
				NodeList<Expression> replaceArguments =
						new NodeList<>(new MethodCallExpr(new NameExpr("Paths"), "get", objectCreationArguments),
								arg0,
								arg1);

				optToPath = Optional.of(new MethodCallExpr(newStaticClass, newStaticMethod, replaceArguments));
			} else if (arg3.isNameExpr()) {
				NodeList<Expression> replaceArguments = new NodeList<>(new MethodCallExpr(arg3, "toPath"), arg0, arg1);

				optToPath = Optional.of(new MethodCallExpr(newStaticClass, newStaticMethod, replaceArguments));
			} else if (arg3.isNullLiteralExpr()) {
				// 'null' is managed specifically as Files.createTempFile does not accept a null as directory
				NodeList<Expression> replaceArguments = new NodeList<>(arg0, arg1);

				optToPath = Optional.of(new MethodCallExpr(newStaticClass, newStaticMethod, replaceArguments));

			} else {
				optToPath = Optional.empty();
			}

		} else {
			optToPath = Optional.empty();
		}

		optToPath.ifPresent(toPath -> {
			methodExp.tryAddImportToParentCompilationUnit(Files.class);

			LOGGER.info("Turning {} into {}", methodExp, toPath);
			methodExp.replace(new MethodCallExpr(toPath, "toFile"));
		});

		return optToPath.isPresent();
	}

}
