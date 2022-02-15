package eu.solven.cleanthat.language.java.rules.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import eu.solven.cleanthat.language.java.rules.annotations.UnchangedMethod;

/**
 * This is useful to investigate a misbehavior over current project file
 *
 * @author Benoit Lacelle
 */
// https://github.com/javaparser/javaparser/issues/3322
// https://github.com/javaparser/javaparser/issues/3330
// TODO ENsure this is trivial to execute
public class ITGetType {
	private static final String SOME_STATIC_CONSTANT = "magic";
	private final String someConstant = "magic";

	@UnchangedMethod
	public static class ReferStaticFieldAsStatic {

		public Object post(String lang) {
			String constant = ITGetType.SOME_STATIC_CONSTANT;
			return lang.equals(constant);
		}
	}

	@UnchangedMethod
	public static class ReferStaticFieldAsField {

		public Object post(String lang) {
			String constant = new ITGetType().SOME_STATIC_CONSTANT;
			return lang.equals(constant);
		}
	}

	@UnchangedMethod
	public static class ReferFieldAsField {

		public Object post(String lang) {
			String constant = new ITGetType().someConstant;
			return lang.equals(constant);
		}
	}

	// Setup symbol solver
	final ParserConfiguration configuration = new ParserConfiguration()
			.setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver())));
	// Setup parser
	final JavaParser parser = new JavaParser(configuration);

	// https://github.com/javaparser/javaparser/issues/1439
	// https://github.com/javaparser/javaparser/issues/1506
	@Test
	public void testResolveType() throws IOException {
		File file = new File("src/test/java/" + ITGetType.class.getName().replace(".", "/") + ".java");
		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}
		String pathAsString = Files.readString(file.toPath());
		CompilationUnit tree = parser.parse(pathAsString).getResult().get();

		tree.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
			List<MethodDeclaration> preMethods = clazz.getMethodsByName("post");
			if (preMethods.size() != 1) {
				return;
			}

			preMethods.forEach(pre -> {
				System.out.println(pre);
				pre.walk(node -> {
					if (!(node instanceof MethodCallExpr)) {
						return;
					}
					MethodCallExpr methodCall = (MethodCallExpr) node;
					if (!methodCall.toString().contains("equals")) {
						return;
					}

					Expression arg0 = methodCall.getArgument(0);
					if (arg0.isNameExpr()) {
						NameExpr nameExpr = arg0.asNameExpr();
						ResolvedValueDeclaration resolved = nameExpr.resolve();
						System.out.println(resolved);

						if (resolved instanceof JavaParserVariableDeclaration) {
							VariableDeclarator declarator =
									((JavaParserVariableDeclaration) resolved).getVariableDeclarator();

							// 'constant = ITGetType.SOME_CONSTANT'
							System.out.println(declarator);
							declarator.getInitializer().ifPresent(expr -> {
								// 'ITGetType.SOME_CONSTANT'
								System.out.println(expr);

								FieldAccessExpr fae = (FieldAccessExpr) expr;

								ResolvedValueDeclaration rvd = fae.resolve();

								System.out.println(rvd.asField().isStatic());

								fae.getScope().calculateResolvedType().asReferenceType().getTypeDeclaration();
							});
						}
					}
				});
			});
		});
	}

	@Test
	public void testResolveType_LeanerStyle() {
		// Source code
		String sourceCode = Stream.of("public class A {                                                     ",
				"            public Object post(String lang) {",
				"                return o.startsWith(Locale.FRANCE.getCountry());",
				"        }",
				"    }").collect(Collectors.joining(System.lineSeparator()));

		CompilationUnit cu = parser.parse(sourceCode).getResult().get();

		cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
			List<MethodDeclaration> preMethods = clazz.getMethodsByName("post");
			if (preMethods.size() != 1) {
				return;
			}
			MethodDeclaration md = preMethods.get(0);

			md.walk(node -> {
				if (!(node instanceof MethodCallExpr)) {
					return;
				}
				MethodCallExpr methodCall = (MethodCallExpr) node;
				if (!methodCall.getOrphanComments().toString().contains("Locale.FRANCE")) {
					return;
				}

				Optional<Expression> optScope = methodCall.getScope();

				Expression scope = optScope.get();

				ResolvedType type = scope.calculateResolvedType(); // 2 - this is how to solve the type

				System.out.println(type.describe());
			});
		});
	}
}
