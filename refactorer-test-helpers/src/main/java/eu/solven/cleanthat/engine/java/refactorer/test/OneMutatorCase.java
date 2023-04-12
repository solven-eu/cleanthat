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
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.engine.java.refactorer.INoOpMutator;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareClasses;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsResources;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareCompilationUnitsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.CompareMethodsAsStrings;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsResource;
import eu.solven.cleanthat.engine.java.refactorer.annotations.UnmodifiedCompilationUnitAsString;
import eu.solven.cleanthat.engine.java.refactorer.meta.ICountMutatorIssues;
import eu.solven.cleanthat.engine.java.refactorer.meta.IReApplyUntilNoop;
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
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.GodClass" })
public class OneMutatorCase<N, R> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OneMutatorCase.class);

	private static final String PRE_METHOD = "pre";
	private static final String PRE_CLASS = "Pre";

	private static final String POST_CONSTANT = "post";
	private static final String POST_CLASS = "Post";

	final JavaParser javaParser;
	final IWalkingMutator<N, R> mutator;

	final IAstTestHelper<N, R> astTestHelper;

	public OneMutatorCase(JavaParser javaParser, IWalkingMutator<N, R> mutator, IAstTestHelper<N, R> astTestHelper) {
		this.javaParser = javaParser;
		this.mutator = mutator;
		this.astTestHelper = astTestHelper;
	}

	public void doCheckUnmodifiedMethod(ClassOrInterfaceDeclaration oneCase) {
		// LOGGER.info("Processing the case: {}", oneCase.getName());
		var post = ATestCases.getMethodWithName(oneCase, PRE_METHOD);
		doCheckUnmodifiedNode(oneCase, post);
	}

	public void doCheckUnmodifiedClass(ClassOrInterfaceDeclaration oneCase) {
		var post = ATestCases.getClassWithName(oneCase, PRE_CLASS);
		doCheckUnmodifiedNode(oneCase, post);
	}

	protected void doCheckUnmodifiedNode(ClassOrInterfaceDeclaration oneCase, Node pre) {
		N preAsAst = astTestHelper.convertToAst(pre);
		var preAsString = astTestHelper.astToString(preAsAst);

		// https://github.com/javaparser/javaparser/issues/3322
		// We prefer not-processing clones as it may lead to dirty issues
		var clonedPre = pre.clone();

		Optional<R> optWalked = mutator.walkAst(preAsAst);
		var walked = optWalked.isPresent();

		if (mutator instanceof INoOpMutator) {
			Assert.assertTrue("INoOpMutator is always walked", walked);
		} else if (walked) {
			var modifiedPreAsString = astTestHelper.resultToString(optWalked.get());

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
		// Assert.assertEquals("No modification on Node.toString()", preAsString, modifiedPreAsString);
		Assert.assertEquals("No modification on Node", clonedPre, pre);

		// https://github.com/javaparser/javaparser/issues/1913
		Assert.assertEquals("No modification on Node.prettyString()",
				LexicalPreservingPrinter.print(clonedPre),
				LexicalPreservingPrinter.print(pre));
	}

	public void doTestMethod(ClassOrInterfaceDeclaration oneCase) {
		var pre = ATestCases.getMethodWithName(oneCase, PRE_METHOD);
		var post = ATestCases.getMethodWithName(oneCase, POST_CONSTANT);

		doCompareExpectedChanges(oneCase, pre, post);
	}

	private <T extends Node> void doCompareExpectedChanges(ClassOrInterfaceDeclaration oneCase, T pre, T post) {
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		// This is done before .toString typically to register LexicalPreservingPrinter
		N asAst = astTestHelper.convertToAst(pre);

		Optional<R> optResult = mutator.walkAst(asAst);

		if (optResult.isPresent() && mutator instanceof IReApplyUntilNoop) {
			var latestResult = optResult.get();
			if (latestResult instanceof Node && asAst instanceof Node) {

				while (true) {
					Optional<R> localMutated = mutator.walkAst((N) latestResult);

					if (localMutated.isPresent()) {
						optResult = localMutated;
						latestResult = optResult.get();
					} else {
						break;
					}
				}
			} else {
				throw new UnsupportedOperationException("Not managed yet");
			}

		}
		var preAsString = astTestHelper.astToString(asAst);

		if (optResult.isEmpty()) {
			Assertions.assertThat(optResult).as("We miss a transformation flag for: " + preAsString).isPresent();
		} else {
			// Rename the method before checking full equality
			// method are lowerCase while classes are camel case
			if (pre instanceof NodeWithSimpleName<?> && post instanceof NodeWithSimpleName<?>) {
				var newName = ((NodeWithSimpleName<?>) post).getName();
				((NodeWithSimpleName<?>) pre).setName(newName);

				if (pre instanceof ClassOrInterfaceDeclaration) {
					pre.findAll(ConstructorDeclaration.class).forEach(cd -> cd.setName(newName));
				}
			}

			// Assert.assertNotEquals("Not a single mutation. Case: " + oneCase, clonedPre, pre);

			var expectedPostAsString = astTestHelper.astToString(astTestHelper.convertToAst(post));
			var actualPostAsString = astTestHelper.resultToString(optResult.get());

			checkChange(oneCase, post, asAst, preAsString, expectedPostAsString, actualPostAsString);
		}

		// Check the mutator is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			// We do not walk the clone as JavaParser has issues inferring types over clones
			var postBeforeWalk = post.clone();
			var postAfterWalk = mutator.walkAst(astTestHelper.convertToAst(post));
			Assert.assertFalse("Not mutating after", postAfterWalk.isPresent());
			Assert.assertEquals("After not mutated", postBeforeWalk, post);
		}
	}

	private void checkChange(ClassOrInterfaceDeclaration oneCase,
			Node post,
			N asAst,
			String preAsString,
			String expectedPostAsString,
			String actualPostAsString) {
		String msg = "Should have mutated " + preAsString
				+ " into "
				+ expectedPostAsString
				+ " but it turned into: "
				+ actualPostAsString
				+ ". The whole testcase is: "
				+ oneCase;
		Assert.assertEquals(msg, expectedPostAsString, actualPostAsString);

		if (preAsString.contains("\"\"\"") || expectedPostAsString.contains("\"\"\"")) {
			// https://github.com/javaparser/javaparser/pull/2320
			// 2 TextBlocks can have the same .toString representation but different underlying value as long as
			// the underlying value are not both stripped
			LOGGER.warn("We skip javaParser Node equality due to stripping in TextBlocks");
		} else if (preAsString.contains("::") || expectedPostAsString.contains("::")) {
			// https://github.com/javaparser/javaparser/pull/2320
			// It can be difficult to provide a TypeExpr given a MethodCallExpr
			LOGGER.warn("We skip javaParser Node equality due to `::` and TypeExpr given a MethodCallExpr");
		} else if (expectedPostAsString.contains("java.util.stream.Stream")
				|| expectedPostAsString.contains("java.util.stream.IntStream")
				|| expectedPostAsString.contains("(double)")
				|| expectedPostAsString.contains("(float)")
				|| expectedPostAsString.contains("(long)")
				|| expectedPostAsString.contains("java.nio.charset.StandardCharsets")) {
			// see ArraysDotStream
			// We build with a NameExpr, while the parser interpret java.util.stream.Stream as a FieldAccessExp
			LOGGER.warn("We skip javaParser Node equality due to `packagedName` (NameExpr vs FieldAccessExp)");
		} else {
			// Some cases leads to failure here: nodes are different while they have the same .toString
			// A Visitor similar to EqualsVisitor, but returning the first different node would be helpful
			if (asAst instanceof Node) {
				// This is supposedly a Javaparser IMutator: we can check the mutated node
				Assert.assertEquals(msg, post, asAst);
			} else {
				LOGGER.debug("This is not a Javaparser IMutator: `pre` is not mutated");
			}
		}

		if (mutator instanceof ICountMutatorIssues) {
			ICountMutatorIssues withCounters = (ICountMutatorIssues) mutator;

			if (!(mutator instanceof IReApplyUntilNoop)) {
				Assertions.assertThat(withCounters.getNbIdempotencyIssues()).describedAs("Idempotency").isEqualTo(0);
			}
			Assertions.assertThat(withCounters.getNbReplaceIssues()).describedAs("tryReplace").isEqualTo(0);
			Assertions.assertThat(withCounters.getNbRemoveIssues()).describedAs("tryRemove").isEqualTo(0);
		}
	}

	public void doCompareTypes(ClassOrInterfaceDeclaration oneCase) {
		// LOGGER.info("Processing the case: {}", oneCase.getName());
		TypeDeclaration<?> pre = oneCase.getMembers()
				.stream()
				.filter(TypeDeclaration.class::isInstance)
				.map(n -> (TypeDeclaration<?>) n)
				.filter(n -> PRE_CLASS.equals(n.getNameAsString()))
				.findAny()
				.get();
		TypeDeclaration<?> post = oneCase.getMembers()
				.stream()
				.filter(TypeDeclaration.class::isInstance)
				.map(n -> (TypeDeclaration<?>) n)
				.filter(n -> POST_CLASS.equals(n.getNameAsString()))
				.findAny()
				.get();
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			mutator.walkAstHasChanged(astTestHelper.convertToAst(pre));
			// Rename the method before checking full equality
			pre.setName(POST_CLASS);
			Assert.assertEquals("Check equality after mutation", post, pre);
		}
		// Check the mutator is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			TypeDeclaration<?> postPost = post.clone();
			mutator.walkAstHasChanged(astTestHelper.convertToAst(postPost));
			Assert.assertEquals("Check idempotency", post, postPost);
		}
	}

	public void doCompareClasses(ClassOrInterfaceDeclaration oneCase) {
		// LOGGER.info("Processing the case: {}", oneCase.getName());

		var qualifiedName = oneCase.getFullyQualifiedName().get();

		// We receive a nested class: we have to manually replace the last '.' into '$' in order to get a
		// nestedClassName compatible with Class.forName
		// https://stackoverflow.com/questions/7007831/instantiate-nested-static-class-using-class-forname
		var lastDot = qualifiedName.lastIndexOf('.');
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
		doCompareExpectedChanges(oneCase, pre.getType(0), post.getType(0));

		// This will also compare imports
		doCompareClasses(pre, post);
	}

	public void doCompareInnerClasses(ClassOrInterfaceDeclaration oneCase) {
		var pre = ATestCases.getClassWithName(oneCase, PRE_CLASS);
		var post = ATestCases.getClassWithName(oneCase, POST_CLASS);

		doCompareExpectedChanges(oneCase, pre, post);
	}

	public void doCompareInnerAnnotations(ClassOrInterfaceDeclaration oneCase) {
		var pre = ATestCases.getAnnotationWithName(oneCase, PRE_CLASS);
		var post = ATestCases.getAnnotationWithName(oneCase, POST_CLASS);

		doCompareExpectedChanges(oneCase, pre, post);
	}

	public void doCompareMethodsAsStrings(ClassOrInterfaceDeclaration oneCase) {
		NormalAnnotationExpr annotation =
				oneCase.getAnnotationByClass(CompareMethodsAsStrings.class).get().asNormalAnnotationExpr();

		var preExpr = annotation.getPairs()
				.stream()
				.filter(p -> PRE_METHOD.equals(p.getNameAsString()))
				.findAny()
				.get()
				.getValue()
				.asStringLiteralExpr();
		var pre = javaParser.parseMethodDeclaration(preExpr.getValue()).getResult().get();
		var postExpr = annotation.getPairs()
				.stream()
				.filter(p -> POST_CONSTANT.equals(p.getNameAsString()))
				.findAny()
				.get()
				.getValue()
				.asStringLiteralExpr();
		var post = javaParser.parseMethodDeclaration(postExpr.getValue()).getResult().get();
		doCompareExpectedChanges(oneCase, pre, post);
	}

	public void doCompareCompilationUnitsAsStrings(ClassOrInterfaceDeclaration testCase,
			CompareCompilationUnitsAsStrings annotation) {
		CompilationUnit pre = throwIfProblems(javaParser.parse(annotation.pre()));
		CompilationUnit post = throwIfProblems(javaParser.parse(annotation.post()));

		LexicalPreservingPrinter.setup(pre);

		doCompareExpectedChanges(testCase, pre, post);
	}

	public void doCheckUnmodifiedCompilationUnitsAsStrings(ClassOrInterfaceDeclaration testCase,
			UnmodifiedCompilationUnitAsString annotation) {
		CompilationUnit pre = throwIfProblems(javaParser.parse(annotation.pre()));
		doCheckUnmodifiedNode(testCase, pre);
	}

	public void doCompareCompilationUnitsAsResources(ClassOrInterfaceDeclaration testCase,
			CompareCompilationUnitsAsResources annotation) {
		String preAsString = PepperResourceHelper.loadAsString(annotation.pre());
		var pre = throwIfProblems(javaParser.parse(preAsString));
		Assertions.assertThat(pre.getTypes()).hasSize(1);

		String postAsString = PepperResourceHelper.loadAsString(annotation.post());
		var post = throwIfProblems(javaParser.parse(postAsString));
		Assertions.assertThat(post.getTypes()).hasSize(1);

		doCompareExpectedChanges(testCase, pre, post);
	}

	public void doCheckUnmodifiedCompilationUnitsAsResources(ClassOrInterfaceDeclaration testCase,
			UnmodifiedCompilationUnitAsResource annotation) {
		String preAsString = PepperResourceHelper.loadAsString(annotation.pre());
		var pre = throwIfProblems(javaParser.parse(preAsString));
		doCheckUnmodifiedNode(testCase, pre.getClassByName("SomeClass").get());
	}

	public static <T> T throwIfProblems(ParseResult<T> parse) {
		if (!parse.isSuccessful()) {
			throw new IllegalArgumentException("Issue parsing the input: " + parse.getProblems());
		}
		return parse.getResult().get();
	}

	public void doCompareClasses(CompilationUnit pre, CompilationUnit post) {
		// Check 'pre' is transformed into 'post'
		// This is generally the most relevant test: to be done first
		{
			var transformed = mutator.walkAstHasChanged(astTestHelper.convertToAst(pre));
			// Rename the class before checking full equality
			pre.getType(0).setName(post.getType(0).getNameAsString());
			Assert.assertEquals("Check are changed", post, pre);
			// We check this after checking comparison with 'post' for greater test result readability
			Assert.assertTrue("We miss a transformation flag for: " + pre, transformed);
		}
		// Check the mutator is impact-less on already clean code
		// This is a less relevant test: to be done later
		{
			// We do not walk the clone as JavaParser has issues inferring types over clones
			var postBeforeWalk = post.clone();
			Assert.assertFalse("Idempotency issue", mutator.walkAstHasChanged(astTestHelper.convertToAst(post)));
			Assert.assertEquals("Idempotency issue", postBeforeWalk, post);
		}
	}
}
