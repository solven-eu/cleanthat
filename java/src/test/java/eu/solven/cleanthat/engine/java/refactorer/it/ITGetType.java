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
package eu.solven.cleanthat.engine.java.refactorer.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserVariableDeclaration;

import eu.solven.cleanthat.engine.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;

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

	@UnmodifiedMethod
	public static class ReferStaticFieldAsStatic {

		public Object post(String lang) {
			var constant = ITGetType.SOME_STATIC_CONSTANT;
			return lang.equals(constant);
		}
	}

	@UnmodifiedMethod
	public static class ReferStaticFieldAsField {

		public Object post(String lang) {
			var constant = new ITGetType().SOME_STATIC_CONSTANT;
			return lang.equals(constant);
		}
	}

	@UnmodifiedMethod
	public static class ReferFieldAsField {

		public Object post(String lang) {
			var constant = new ITGetType().someConstant;
			return lang.equals(constant);
		}
	}

	final JavaParser parser = JavaRefactorer.makeDefaultJavaParser(JavaRefactorer.JAVAPARSER_JRE_ONLY);

	// https://github.com/javaparser/javaparser/issues/1439
	// https://github.com/javaparser/javaparser/issues/1506
	@Test
	public void testResolveType() throws IOException {
		var file = new File("src/test/java/" + ITGetType.class.getName().replace(".", "/") + ".java");
		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}
		var pathAsString = Files.readString(file.toPath());
		var tree = parser.parse(pathAsString).getResult().get();

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
					var methodCall = (MethodCallExpr) node;
					if (!methodCall.toString().contains("equals")) {
						return;
					}

					var arg0 = methodCall.getArgument(0);
					if (arg0.isNameExpr()) {
						var nameExpr = arg0.asNameExpr();
						var resolved = nameExpr.resolve();
						System.out.println(resolved);

						if (resolved instanceof JavaParserVariableDeclaration) {
							var declarator = ((JavaParserVariableDeclaration) resolved).getVariableDeclarator();

							// 'constant = ITGetType.SOME_CONSTANT'
							System.out.println(declarator);
							declarator.getInitializer().ifPresent(expr -> {
								// 'ITGetType.SOME_CONSTANT'
								System.out.println(expr);

								var fae = (FieldAccessExpr) expr;

								var rvd = fae.resolve();

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
		var sourceCode = Stream.of("public class A {                                                     ",
				"            public Object post(String lang) {",
				"                return o.startsWith(Locale.FRANCE.getCountry());",
				"        }",
				"    }").collect(Collectors.joining(System.lineSeparator()));

		var cu = parser.parse(sourceCode).getResult().get();

		cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
			List<MethodDeclaration> preMethods = clazz.getMethodsByName("post");
			if (preMethods.size() != 1) {
				return;
			}
			var md = preMethods.get(0);

			md.walk(node -> {
				if (!(node instanceof MethodCallExpr)) {
					return;
				}
				var methodCall = (MethodCallExpr) node;
				if (!methodCall.getOrphanComments().toString().contains("Locale.FRANCE")) {
					return;
				}

				Optional<Expression> optScope = methodCall.getScope();

				var scope = optScope.get();

				var type = scope.calculateResolvedType(); // 2 - this is how to solve the type

				System.out.println(type.describe());
			});
		});
	}
}
