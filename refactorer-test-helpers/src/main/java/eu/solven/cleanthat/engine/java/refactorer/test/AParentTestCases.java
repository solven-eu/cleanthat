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
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerAnnotations;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareInnerClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethods;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethodsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareTypes;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedMethod;
import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkingMutator;

public abstract class AParentTestCases<N, NN extends N, R> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AParentTestCases.class);

	protected static List<ClassOrInterfaceDeclaration> getAllCases(CompilationUnit compilationUnit) {
		return compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(CompareTypes.class).isPresent()
						|| c.getAnnotationByClass(CompareMethods.class).isPresent()
						|| c.getAnnotationByClass(CompareClasses.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedMethod.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerClasses.class).isPresent()
						|| c.getAnnotationByClass(CompareInnerAnnotations.class).isPresent()
						|| c.getAnnotationByClass(CompareMethodsAsStrings.class).isPresent()
						|| c.getAnnotationByClass(CompareCompilationUnitsAsStrings.class).isPresent()
						|| c.getAnnotationByClass(UnmodifiedCompilationUnitAsString.class).isPresent());
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

	protected void doCheckUnmodified(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		LOGGER.info("Processing the case: {}", oneCase.getName());
		MethodDeclaration post = getMethodWithName(oneCase, "pre");
		doCheckUnmodified(transformer, oneCase, post);
	}

	protected void doCheckUnmodified(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase, Node pre) {
		LexicalPreservingPrinter.setup(pre);
		// https://github.com/javaparser/javaparser/issues/3322
		// We prefer not-processing clones as it may lead to dirty issues
		Node clonedPost = pre.clone();
		boolean walked = transformer.walkAstHasChanged(convertToAst(pre));
		if (transformer instanceof INoOpMutator) {
			Assert.assertTrue("INoOpMutator is always walked", walked);
		} else if (walked) {
			Assert.assertFalse(
					"Should not have mutated " + clonedPost
							+ " but it turned into: "
							+ pre
							+ ". The whole testcase is: "
							+ oneCase,
					walked);
		}
		Assert.assertEquals(clonedPost, pre);
		Assert.assertEquals(clonedPost.toString(), pre.toString());
		// https://github.com/javaparser/javaparser/issues/1913
		Assert.assertEquals(LexicalPreservingPrinter.print(clonedPost).toString(),
				LexicalPreservingPrinter.print(pre).toString());
	}

	protected void doTestMethod(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		LOGGER.info("Processing the case: {}", oneCase.getName());
		MethodDeclaration pre = getMethodWithName(oneCase, "pre");
		MethodDeclaration post = getMethodWithName(oneCase, "post");

		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	private <T extends Node & NodeWithSimpleName<?>> void doCompareExpectedChanges(IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase,
			T pre,
			T post) {
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			Node clonedPre = pre.clone();
			boolean transformed = transformer.walkAstHasChanged(convertToAst(pre));
			// Rename the method before checking full equality
			// method are lowerCase while classes are camel case
			pre.setName(post.getName());

			// Assert.assertNotEquals("Not a single mutation. Case: " + oneCase, clonedPre, pre);
			Assert.assertEquals("Should have mutated " + clonedPre
					+ " into "
					+ post
					+ " but it turned into: "
					+ pre
					+ ". The whole testcase is: "
					+ oneCase, post, pre);
			// We check this after checking comparison with 'post' for greater test result readability
			Assert.assertTrue("We miss a transformation flag for: " + pre, transformed);
		}
		// Check the transformer is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			// We do not walk the clone as JavaParser has issues inferring types over clones
			Node postBeforeWalk = post.clone();
			Assert.assertFalse("Unexpected transformation on code just transformed",
					transformer.walkAstHasChanged(convertToAst(post)));
			Assert.assertEquals(postBeforeWalk, post);
		}
	}

	protected void doCompareTypes(IWalkingMutator<N, R> transformer, ClassOrInterfaceDeclaration oneCase) {
		LOGGER.info("Processing the case: {}", oneCase.getName());
		TypeDeclaration<?> pre = oneCase.getMembers()
				.stream()
				.filter(n -> n instanceof TypeDeclaration)
				.map(n -> (TypeDeclaration<?>) n)
				.filter(n -> "Pre".equals(n.getNameAsString()))
				.findAny()
				.get();
		TypeDeclaration<?> post = oneCase.getMembers()
				.stream()
				.filter(n -> n instanceof TypeDeclaration)
				.map(n -> (TypeDeclaration<?>) n)
				.filter(n -> "Post".equals(n.getNameAsString()))
				.findAny()
				.get();
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			transformer.walkAstHasChanged(convertToAst(pre));
			// Rename the method before checking full equality
			pre.setName("Post");
			Assert.assertEquals(post, pre);
		}
		// Check the transformer is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			TypeDeclaration<?> postPost = post.clone();
			transformer.walkAstHasChanged(convertToAst(postPost));
			Assert.assertEquals(post, postPost);
		}
	}

	protected void doCompareClasses(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		LOGGER.info("Processing the case: {}", oneCase.getName());

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
		LOGGER.info("Processing the case: {}", oneCase.getName());
		ClassOrInterfaceDeclaration pre = getClassWithName(oneCase, "Pre");
		ClassOrInterfaceDeclaration post = getClassWithName(oneCase, "Post");

		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	protected void doCompareInnerAnnotations(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		LOGGER.info("Processing the case: {}", oneCase.getName());
		AnnotationDeclaration pre = getAnnotationWithName(oneCase, "Pre");
		AnnotationDeclaration post = getAnnotationWithName(oneCase, "Post");

		doCompareExpectedChanges(transformer, oneCase, pre, post);
	}

	public void doCompareMethodsAsStrings(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration oneCase) {
		LOGGER.info("Processing the case: {}", oneCase.getName());

		NormalAnnotationExpr annotation =
				oneCase.getAnnotationByClass(CompareMethodsAsStrings.class).get().asNormalAnnotationExpr();

		StringLiteralExpr preExpr = annotation.getPairs()
				.stream()
				.filter(p -> "pre".equals(p.getNameAsString()))
				.findAny()
				.get()
				.getValue()
				.asStringLiteralExpr();
		MethodDeclaration pre = javaParser.parseMethodDeclaration(preExpr.getValue()).getResult().get();
		StringLiteralExpr postExpr = annotation.getPairs()
				.stream()
				.filter(p -> "post".equals(p.getNameAsString()))
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
		LOGGER.info("Processing the case: {}", testCase.getName());

		CompilationUnit pre = javaParser.parse(annotation.pre()).getResult().get();
		CompilationUnit post = javaParser.parse(annotation.post()).getResult().get();
		doCompareExpectedChanges(transformer,
				testCase,
				pre.getClassByName("SomeClass").get(),
				post.getClassByName("SomeClass").get());
	}

	public void doCheckUnmodifiedCompilationUnitsAsStrings(JavaParser javaParser,
			IWalkingMutator<N, R> transformer,
			ClassOrInterfaceDeclaration testCase,
			UnmodifiedCompilationUnitAsString annotation) {
		LOGGER.info("Processing the case: {}", testCase.getName());

		CompilationUnit pre = javaParser.parse(annotation.pre()).getResult().get();
		doCheckUnmodified(transformer, testCase, pre.getClassByName("SomeClass").get());
	}

	protected void doCompareClasses(IWalkingMutator<N, R> transformer, CompilationUnit pre, CompilationUnit post) {

		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			boolean transformed = transformer.walkAstHasChanged(convertToAst(pre));
			// Rename the class before checking full equality
			pre.getType(0).setName(post.getType(0).getNameAsString());
			Assert.assertEquals(post, pre);
			// We check this after checking comparison with 'post' for greater test result readability
			Assert.assertTrue("We miss a transformation flag for: " + pre, transformed);
		}
		// Check the transformer is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			// We do not walk the clone as JavaParser has issues inferring types over clones
			CompilationUnit postBeforeWalk = post.clone();
			Assert.assertFalse(transformer.walkAstHasChanged(convertToAst(post)));
			Assert.assertEquals(postBeforeWalk, post);
		}
	}

	protected abstract N convertToAst(Node pre);
}
