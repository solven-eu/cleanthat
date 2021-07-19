package io.cormoran.cleanthat.rules.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.rules.cases.annotations.UnchangedMethod;

/**
 * This is useful to investigate a misbehavior over current project file
 * 
 * @author Benoit Lacelle
 *
 */
public class ITGetType {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITGetType.class);

	@UnchangedMethod
	public static class CheckStartsWith {
		public Object post(String o) {
			return o.startsWith("Youpi");
		}
	}

	@Test
	public void testResolveType() throws IOException {
		File file = new File("src/test/java/" + ITGetType.class.getName().replace(".", "/") + ".java");

		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}

		String pathAsString = Files.readString(file.toPath());

		CompilationUnit tree = StaticJavaParser.parse(pathAsString);

		CombinedTypeSolver ts = new CombinedTypeSolver();
		ts.add(new ReflectionTypeSolver());
		JavaParserFacade parser = JavaParserFacade.get(ts);

		tree.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {

			List<MethodDeclaration> preMethods = clazz.getMethodsByName("post");
			if (preMethods.size() != 1) {
				return;
			}
			MethodDeclaration pre = preMethods.get(0);

			pre = pre.clone();

			pre.walk(node -> {
				if (!(node instanceof MethodCallExpr)) {
					return;
				}
				MethodCallExpr methodCall = (MethodCallExpr) node;
				if (!methodCall.toString().equals("o.startsWith(\"Youpi\")")) {
					return;
				}

				Optional<Expression> optScope = methodCall.getScope();

				Expression scope = optScope.get();

				ResolvedType type = parser.getType(scope);

				LOGGER.info("Type: {}", type);
			});
		});
	}
}
