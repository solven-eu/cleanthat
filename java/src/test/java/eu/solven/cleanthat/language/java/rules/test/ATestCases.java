package eu.solven.cleanthat.language.java.rules.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.language.java.rules.NoOpJavaParserRule;
import eu.solven.cleanthat.language.java.rules.annotations.CompareClasses;
import eu.solven.cleanthat.language.java.rules.annotations.CompareMethods;
import eu.solven.cleanthat.language.java.rules.annotations.CompareTypes;
import eu.solven.cleanthat.language.java.rules.annotations.UnchangedMethod;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;

public class ATestCases {

	private static final Logger LOGGER = LoggerFactory.getLogger(ATestCases.class);

	protected void testCasesIn(ACases cases) throws IOException {
		testCasesIn(cases.getClass(), cases.getTransformer());
	}

	protected void testCasesIn(Class<?> casesClass, IClassTransformer transformer) throws IOException {
		String path = LocalClassTestHelper.loadClassAsString(casesClass);

		JavaParser javaParser = RulesJavaMutator.makeDefaultJavaParser(transformer.isJreOnly());
		CompilationUnit compilationUnit = javaParser.parse(path).getResult().get();

		checkMethodCases(transformer, compilationUnit);
		checkMethodUnchangedCases(transformer, compilationUnit);
		checkTypeCases(transformer, compilationUnit);
		checkClasses(javaParser, transformer, compilationUnit);
	}

	private void checkMethodCases(IClassTransformer transformer, CompilationUnit compilationUnit) {
		List<ClassOrInterfaceDeclaration> methodCases = compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(CompareMethods.class).isPresent());
		methodCases.forEach(oneCase -> {
			if (oneCase.getAnnotationByClass(Ignore.class).isPresent()) {
				return;
			}
			LOGGER.info("Processing the case: {}", oneCase.getName());
			MethodDeclaration pre = getMethodWithName(oneCase, "pre");
			MethodDeclaration post = getMethodWithName(oneCase, "post");

			// https://github.com/javaparser/javaparser/issues/3322
			// We prefer not-processing clones as it may lead to dirty issues
			MethodDeclaration clonedPre = pre.clone();

			// Check 'pre' is transformed into 'post'
			// This is generally the most relevant test: to be done first
			{
				boolean transformed = transformer.walkNode(pre);
				// Rename the method before checking full equality
				pre.setName("post");
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
				MethodDeclaration postBeforeWalk = post.clone();
				Assert.assertFalse(transformer.walkNode(post));
				Assert.assertEquals(postBeforeWalk, post);
			}
		});
	}

	private void checkMethodUnchangedCases(IClassTransformer transformer, CompilationUnit compilationUnit) {
		List<ClassOrInterfaceDeclaration> unchangedMethods = compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(UnchangedMethod.class).isPresent());
		unchangedMethods.stream().forEach(oneCase -> {
			if (oneCase.getAnnotationByClass(Ignore.class).isPresent()) {
				return;
			}
			LOGGER.info("Processing the case: {}", oneCase.getName());
			MethodDeclaration post = getMethodWithName(oneCase, "post");
			// Check the transformer is impact-less on already clean code
			// This is a less relevant test: to be done later
			{
				LexicalPreservingPrinter.setup(post);

				// https://github.com/javaparser/javaparser/issues/3322
				// We prefer not-processing clones as it may lead to dirty issues
				MethodDeclaration clonedPost = post.clone();
				boolean walked = transformer.walkNode(post);
				if (transformer instanceof NoOpJavaParserRule) {
					Assert.assertTrue("NoOpJavaParserRule is always walked", walked);
				} else {
					Assert.assertFalse(
							"Should not have mutated " + post
									+ " but it turned into: "
									+ clonedPost
									+ ". The whole testcase is: "
									+ oneCase,
							walked);
				}
				Assert.assertEquals(clonedPost, post);
				Assert.assertEquals(clonedPost.toString(), post.toString());

				// https://github.com/javaparser/javaparser/issues/1913
				Assert.assertEquals(LexicalPreservingPrinter.print(clonedPost).toString(),
						LexicalPreservingPrinter.print(post).toString());
			}
		});
	}

	private void checkTypeCases(IClassTransformer transformer, CompilationUnit compilationUnit) {
		List<ClassOrInterfaceDeclaration> typeCases = compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(CompareTypes.class).isPresent());
		typeCases.forEach(oneCase -> {
			if (oneCase.getAnnotationByClass(Ignore.class).isPresent()) {
				return;
			}
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
				transformer.walkNode(pre);
				// Rename the method before checking full equality
				pre.setName("Post");
				Assert.assertEquals(post, pre);
			}
			// Check the transformer is impact-less on already clean code
			// This is a less relevant test: to be done later
			{
				TypeDeclaration<?> postPost = post.clone();
				transformer.walkNode(postPost);
				Assert.assertEquals(post, postPost);
			}
		});
	}

	private void checkClasses(JavaParser javaParser, IClassTransformer transformer, CompilationUnit compilationUnit) {
		List<ClassOrInterfaceDeclaration> typeCases = compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
				c -> c.getAnnotationByClass(CompareClasses.class).isPresent());
		typeCases.forEach(oneCase -> {
			if (oneCase.getAnnotationByClass(Ignore.class).isPresent()) {
				return;
			}
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

			// Check 'pre' is transformed into 'post'
			// This is generally the most relevant test: to be done first
			{
				boolean transformed = transformer.walkNode(pre);
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
				Assert.assertFalse(transformer.walkNode(post));
				Assert.assertEquals(postBeforeWalk, post);
			}
		});
	}

	public static MethodDeclaration getMethodWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<MethodDeclaration> preMethods = oneCase.getMethodsByName(name);
		if (preMethods.size() != 1) {
			throw new IllegalStateException("Expected one and only one '" + name + "' method in " + oneCase);
		}
		MethodDeclaration pre = preMethods.get(0);
		return pre;
	}
}
