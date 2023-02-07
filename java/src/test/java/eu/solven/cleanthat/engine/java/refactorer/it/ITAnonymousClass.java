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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class ITAnonymousClass {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITAnonymousClass.class);
	private static final String eol = System.lineSeparator();

	public static class CaseAnonymousClass_HashMap {

		public Object post() {
			return new HashMap<String, List<String>>() {
				private static final long serialVersionUID = 1L;

				{
					this.put("k", List.of());
				}

				@Override
				public List<String> put(String key, List<String> value) {
					return super.put(key, value);
				}
			};
		}
	}

	public static class CaseAnonymousClass_FunctionalInterface {

		public Object post() {
			return new Function<String, List<String>>() {

				@Override
				public List<String> apply(String t) {
					return Arrays.asList(t);
				}

			};
		}
	}

	public static class CaseAnonymousClass_Interface {

		public Object post() {
			return new Function<String, List<String>>() {

				@Override
				public List<String> apply(String t) {
					return Arrays.asList(t);
				}

				public void anotherMethod() {
					System.out.println("Usable by reflection?");
				}

			};
		}
	}

	// Setup symbol solver
	final ParserConfiguration configuration = new ParserConfiguration()
			.setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver())));
	// Setup parser
	final JavaParser parser = new JavaParser(configuration);

	@Test
	public void testResolveType() throws IOException {
		File file = new File("src/test/java/" + ITAnonymousClass.class.getName().replace(".", "/") + ".java");
		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}
		String pathAsString = Files.readString(file.toPath());
		CompilationUnit tree = parser.parse(pathAsString).getResult().get();

		tree.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
			List<MethodDeclaration> preMethods = clazz.getMethodsByName("post");

			preMethods.forEach(pre -> {
				pre.walk(node -> {
					if (node instanceof NodeWithTypeArguments) {
						Optional<Node> optParentNode = node.getParentNode();

						if (optParentNode.isPresent() && optParentNode.get() instanceof ObjectCreationExpr) {
							ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) optParentNode.get();
							Optional<ResolvedReferenceTypeDeclaration> optTypeDeclaration =
									objectCreationExpr.calculateResolvedType().asReferenceType().getTypeDeclaration();
							if (optTypeDeclaration.isEmpty()) {
								return;
							}

							if (objectCreationExpr.getAnonymousClassBody().isPresent()) {
								// https://github.com/javaparser/javaparser/issues/3333#issuecomment-893572693
								LOGGER.info("Anonymous Class");
							}

							ResolvedReferenceTypeDeclaration typeDecl = optTypeDeclaration.get();
							if (typeDecl.isAnonymousClass()) {
								ResolvedClassDeclaration asClass = typeDecl.asClass();
								LOGGER.info("anon={} {}{}", asClass.isAnonymousClass(), eol, optParentNode.get());
							} else if (typeDecl.isClass()) {
								ResolvedClassDeclaration asClass = typeDecl.asClass();
								LOGGER.info("anon={} {}{}", asClass.isAnonymousClass(), eol, optParentNode.get());
							} else if (typeDecl.isFunctionalInterface()) {
								ResolvedInterfaceDeclaration asClass = typeDecl.asInterface();
								LOGGER.info("anon={} {}{}", asClass.isAnonymousClass(), eol, optParentNode.get());
							} else if (typeDecl.isInterface()) {
								ResolvedInterfaceDeclaration asClass = typeDecl.asInterface();
								LOGGER.info("anon={} {}{}", asClass.isAnonymousClass(), eol, optParentNode.get());
							} else {
								System.out.println("?");
							}
						}
					}
				});
			});
		});
	}
}
