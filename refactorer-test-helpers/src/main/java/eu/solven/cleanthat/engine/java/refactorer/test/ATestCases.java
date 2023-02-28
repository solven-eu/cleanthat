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
package eu.solven.cleanthat.engine.java.refactorer.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.engine.java.refactorer.INoOpMutator;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethodsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;
import eu.solven.pepper.resource.PepperResourceHelper;

/**
 * Base class for Cleanthat testing framework
 * 
 * @author Benoit Lacelle
 *
 * @param <N>
 * @param <R>
 */
public abstract class ATestCases<N, R> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ATestCases.class);

	private static final String PRE_METHOD = "pre";
	private static final String PRE_CLASS = "Pre";

	private static final String POST_CONSTANT = "post";
	private static final String POST_CLASS = "Post";

	protected static List<ClassOrInterfaceDeclaration> getAllCases(CompilationUnit compilationUnit) {
		return compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(CompareTypes.class).isPresent()
						|| c.getAnnotationByClass(CompareMethods.class).isPresent()
						|| c.getAnnotationByClass(CompareClasses.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedMethod.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerClasses.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedInnerClasses.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerAnnotations.class).isPresent()
						|| c.getAnnotationByClass(CompareMethodsAsStrings.class).isPresent()
						|| c.getAnnotationByClass(CompareCompilationUnitsAsStrings.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedCompilationUnitAsString.class).isPresent()
						|| c.getAnnotationByClass(CompareCompilationUnitsAsResources.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedCompilationUnitsAsResources.class).isPresent());
	}

	public static MethodDeclaration getMethodWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<MethodDeclaration> preMethods = oneCase.getMethodsByName(name);
		if (preMethods.size() != 1) {
			throw new IllegalStateException("Expected one and only one '" + name + "' method in " + oneCase);
		}
		MethodDeclaration pre = preMethods.get(0);
		return pre;
	}

	public static ClassOrInterfaceDeclaration getClassWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<ClassOrInterfaceDeclaration> matching =
				oneCase.findAll(ClassOrInterfaceDeclaration.class, n -> name.equals(n.getNameAsString()));

		if (matching.size() != 1) {
			throw new IllegalStateException(
					"We expected a single interface/class named '" + name + "' but they were: " + matching.size());
		}

		return matching.get(0);
	}

	public static AnnotationDeclaration getAnnotationWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<AnnotationDeclaration> matching =
				oneCase.findAll(AnnotationDeclaration.class, n -> name.equals(n.getNameAsString()));

		if (matching.size() != 1) {
			throw new IllegalStateException(
					"We expected a single annotation named '" + name + "' but they were: " + matching.size());
		}

		return matching.get(0);
	}

	protected void doCheckUnmodifiedMethod(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		// LOGGER.info("Processing the case: {}", oneCase.getName());
		MethodDeclaration post = getMethodWithName(oneCase, PRE_METHOD);
		doCheckUnmodifiedNode(transformer, oneCase, post);
	}

	protected void doCheckUnmodifiedClass(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		ClassOrInterfaceDeclaration post = getClassWithName(oneCase, PRE_CLASS);
		doCheckUnmodifiedNode(transformer, oneCase, post);
	}

	protected void doCheckUnmodifiedNode(IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase,
			Node pre) {
		LexicalPreservingPrinter.setup(pre);
		String preAsString = toString(pre);

		// https://github.com/javaparser/javaparser/issues/3322
		// We prefer not-processing clones as it may lead to dirty issues
		Node clonedPre = pre.clone();

		Optional<R> optWalked = transformer.walkAst(convertToAst(pre));
		boolean walked = optWalked.isPresent();

		String modifiedPreAsString = toString(pre);

		if (transformer instanceof INoOpMutator) {
			Assert.assertTrue("INoOpMutator is always walked", walked);
		} else if (walked) {
			Assert.assertEquals("Should not have mutated " + preAsString
					+ " but it turned into: "
					+ preAsString
					+ ". The whole testcase is: "
					+ oneCase, preAsString, modifiedPreAsString);

			Assert.assertFalse(
					"Should not have mutated " + clonedPre
							+ " but it turned into: "
							+ pre
							+ ". The whole testcase is: "
							+ oneCase,
					walked);
		}
		Assert.assertEquals("No modification on Node.toString()", preAsString, modifiedPreAsString);
		Assert.assertEquals("No modification on Node", clonedPre, pre);

		// https://github.com/javaparser/javaparser/issues/1913
		Assert.assertEquals("No modification on Node.prettyString()",
				LexicalPreservingPrinter.print(clonedPre),
				LexicalPreservingPrinter.print(pre));
	}

	protected void doTestMethod(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		MethodDeclaration pre = getMethodWithName(oneCase, PRE_METHOD);
		MethodDeclaration post = getMethodWithName(oneCase, POST_CONSTANT);

		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	private <T extends Node> void doCompareExpectedChanges(IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase,
			T pre,
			T post) {
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			Node clonedPre = pre.clone();
			String preAsString = toString(clonedPre);

			Optional<R> optResult = transformer.walkAst(convertToAst(pre));

			if (optResult.isEmpty()) {
				Assertions.assertThat(optResult).as("We miss a transformation flag for: " + preAsString).isPresent();
			} else {
				// Rename the method before checking full equality
				// method are lowerCase while classes are camel case
				if (pre instanceof NodeWithSimpleName<?> && post instanceof NodeWithSimpleName<?>) {
					((NodeWithSimpleName<?>) pre).setName(((NodeWithSimpleName<?>) post).getName());
				}

				// Assert.assertNotEquals("Not a single mutation. Case: " + oneCase, clonedPre, pre);

				String expectedPost = toString(post);
				String msg = "Should have mutated " + preAsString
						+ " into "
						+ expectedPost
						+ " but it turned into: "
						+ pre
						+ ". The whole testcase is: "
						+ oneCase;
				String actualPost = toString(optResult.get());
				Assert.assertEquals(msg, expectedPost, actualPost);

				if (preAsString.contains("\"\"\"") || expectedPost.contains("\"\"\"")) {
					// https://github.com/javaparser/javaparser/pull/2320
					// 2 TextBlocks can have the same .toString representation but different underlying value as long as
					// the underlying value are not both stripped
					LOGGER.warn("We skip javaParser Node equality due to stripping in TextBlocks");
				} else if (preAsString.contains("::") || expectedPost.contains("::")) {
					// https://github.com/javaparser/javaparser/pull/2320
					// It can be difficult to provide a TypeExpr given a MethodCallExpr
					LOGGER.warn("We skip javaParser Node equality due to `::` anf TypeExpr given a MethodCallExpr");
				} else {
					// Some cases leads to failure here: nodes are different while they have the same .toString
					// A Visitor similar to EqualsVisiyot, but returning the first different node would be helpful
					Assert.assertEquals(msg, post, pre);
				}
			}
		}
		// Check the transformer is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			// We do not walk the clone as JavaParser has issues inferring types over clones
			Node postBeforeWalk = post.clone();
			boolean postAfterWalk = transformer.walkAstHasChanged(convertToAst(post));
			Assert.assertFalse("Not mutating after", postAfterWalk);
			Assert.assertEquals("After not mutated", postBeforeWalk, post);
		}
	}

	protected abstract <T extends Node> String toString(T post);

	protected abstract String toString(R post);

	protected void doCompareTypes(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		// LOGGER.info("Processing the case: {}", oneCase.getName());
		TypeDeclaration<?> pre = oneCase.getMembers()
				.stream()
				.filter(n -> n instanceof TypeDeclaration)
				.map(n -> (TypeDeclaration<?>) n)
				.filter(n -> PRE_CLASS.equals(n.getNameAsString()))
				.findAny()
				.get();
		TypeDeclaration<?> post = oneCase.getMembers()
				.stream()
				.filter(n -> n instanceof TypeDeclaration)
				.map(n -> (TypeDeclaration<?>) n)
				.filter(n -> POST_CLASS.equals(n.getNameAsString()))
				.findAny()
				.get();
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			transformer.walkAstHasChanged(convertToAst(pre));
			// Rename the method before checking full equality
			pre.setName(POST_CLASS);
			Assert.assertEquals("Check equality after mutation", post, pre);
		}
		// Check the transformer is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			TypeDeclaration<?> postPost = post.clone();
			transformer.walkAstHasChanged(convertToAst(postPost));
			Assert.assertEquals("Check idempotency", post, postPost);
		}
	}

	protected void doCompareClasses(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		// LOGGER.info("Processing the case: {}", oneCase.getName());

		String qualifiedName = oneCase.getFullyQualifiedName().get();

		// We receive a nested class: we have to manually replace the last '.' into '$' in order to get a
		// nestedClassName compatible with Class.forName
		// https://stackoverflow.com/questions/7007831/instantiate-nested-static-class-using-class-forname
		int lastDot = qualifiedName.lastIndexOf('.');
		qualifiedName = qualifiedName.substring(0, lastDot) + "$" + qualifiedName.substring(lastDot + 1);

		Class<?> clazz;
		try {
			clazz = Class.forName(qualifiedName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Issue with className=" + qualifiedName, e);
		}

		CompareClasses annotation = clazz.getAnnotation(CompareClasses.class);

		CompilationUnit pre;
		CompilationUnit post;
		try {
			pre = javaParser.parse(LocalClassTestHelper.localClassAsPath(annotation.pre())).getResult().get();
			post = javaParser.parse(LocalClassTestHelper.localClassAsPath(annotation.post())).getResult().get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// Ensure both classes reports a single type
		// We assume this covers 99% of world codebase
		Assertions.assertThat(pre.getTypes()).hasSize(1);
		Assertions.assertThat(post.getTypes()).hasSize(1);

		// Re-use common comparison method, but it will process the type: it will not process imports
		doCompareExpectedChanges(transformer, oneCase, pre.getType(0), post.getType(0));

		// This will also compare imports
		doCompareClasses(transformer, pre, post);
	}

	protected void doCompareInnerClasses(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		ClassOrInterfaceDeclaration pre = getClassWithName(oneCase, PRE_CLASS);
		ClassOrInterfaceDeclaration post = getClassWithName(oneCase, POST_CLASS);

		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	protected void doCompareInnerAnnotations(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		AnnotationDeclaration pre = getAnnotationWithName(oneCase, PRE_CLASS);
		AnnotationDeclaration post = getAnnotationWithName(oneCase, POST_CLASS);

		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	public void doCompareMethodsAsStrings(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		NormalAnnotationExpr annotation =
				oneCase.getAnnotationByClass(CompareMethodsAsStrings.class).get().asNormalAnnotationExpr();

		StringLiteralExpr preExpr = annotation.getPairs()
				.stream()
				.filter(p -> PRE_METHOD.equals(p.getNameAsString()))
				.findAny()
				.get()
				.getValue()
				.asStringLiteralExpr();
		MethodDeclaration pre = javaParser.parseMethodDeclaration(preExpr.getValue()).getResult().get();
		StringLiteralExpr postExpr = annotation.getPairs()
				.stream()
				.filter(p -> POST_CONSTANT.equals(p.getNameAsString()))
				.findAny()
				.get()
				.getValue()
				.asStringLiteralExpr();
		MethodDeclaration post = javaParser.parseMethodDeclaration(postExpr.getValue()).getResult().get();
		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	public void doCompareCompilationUnitsAsStrings(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration testCase,
			CompareCompilationUnitsAsStrings annotation) {
		CompilationUnit pre = javaParser.parse(annotation.pre()).getResult().get();
		CompilationUnit post = javaParser.parse(annotation.post()).getResult().get();

		doCompareExpectedChanges(transformer, testCase, pre, post);
	}

	public void doCheckUnmodifiedCompilationUnitsAsStrings(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration testCase,
			UnmodifiedCompilationUnitAsString annotation) {
		CompilationUnit pre = javaParser.parse(annotation.pre()).getResult().get();
		doCheckUnmodifiedNode(transformer, testCase, pre);
	}

	public void doCompareCompilationUnitsAsResources(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration testCase,
			CompareCompilationUnitsAsResources annotation) {
		String preAsString = PepperResourceHelper.loadAsString(annotation.pre());
		CompilationUnit pre = javaParser.parse(preAsString).getResult().get();
		Assertions.assertThat(pre.getTypes()).hasSize(1);

		String postAsString = PepperResourceHelper.loadAsString(annotation.post());
		CompilationUnit post = javaParser.parse(postAsString).getResult().get();
		Assertions.assertThat(post.getTypes()).hasSize(1);

		doCompareExpectedChanges(transformer, testCase, pre, post);
	}

	public void doCheckUnmodifiedCompilationUnitsAsResources(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration testCase,
			UnmodifiedCompilationUnitsAsResources annotation) {
		String preAsString = PepperResourceHelper.loadAsString(annotation.pre());
		CompilationUnit pre = javaParser.parse(preAsString).getResult().get();
		doCheckUnmodifiedNode(transformer, testCase, pre.getClassByName("SomeClass").get());
	}

	protected void doCompareClasses(IWalkingMutator<N, R> transformer, CompilationUnit pre, CompilationUnit post) {
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			boolean transformed = transformer.walkAstHasChanged(convertToAst(pre));
			// Rename the class before checking full equality
			pre.getType(0).setName(post.getType(0).getNameAsString());
			Assert.assertEquals("Check are changed", post, pre);
			// We check this after checking comparison with 'post' for greater test result readability
			Assert.assertTrue("We miss a transformation flag for: " + pre, transformed);
		}
		// Check the transformer is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			// We do not walk the clone as JavaParser has issues inferring types over clones
			CompilationUnit postBeforeWalk = post.clone();
			Assert.assertFalse("Idempotency issue", transformer.walkAstHasChanged(convertToAst(post)));
			Assert.assertEquals("Idempotency issue", postBeforeWalk, post);
		}
	}

	protected abstract N convertToAst(Node pre);
}
