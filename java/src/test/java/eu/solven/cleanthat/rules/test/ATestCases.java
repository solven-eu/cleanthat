package eu.solven.cleanthat.rules.test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import eu.solven.cleanthat.rules.cases.annotations.CompareMethods;
import eu.solven.cleanthat.rules.cases.annotations.CompareTypes;
import eu.solven.cleanthat.rules.cases.annotations.UnchangedMethod;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class ATestCases {
	private static final Logger LOGGER = LoggerFactory.getLogger(ATestCases.class);

	protected void testCasesIn(ACases cases) throws IOException {
		testCasesIn(cases.getClass(), cases.getTransformer());
	}

	protected void testCasesIn(Class<?> casesClass, IClassTransformer transformer) throws IOException {
		Path srcMainJava = getProjectTestSourceCode();
		// https://stackoverflow.com/questions/3190301/obtaining-java-source-code-from-class-name
		String path = casesClass.getName().replaceAll("\\.", "/") + ".java";
		CompilationUnit compilationUnit = StaticJavaParser.parse(srcMainJava.resolve(path));

		checkMethodCases(transformer, compilationUnit);
		checkMethodUnchangedCases(transformer, compilationUnit);
		checkTypeCases(transformer, compilationUnit);
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

			// Check 'pre' is transformed into 'post'
			// This is generally the most relevant test: to be done first
			{
				boolean transformed = transformer.walkNode(pre);
				// Rename the method before checking full equality
				pre.setName("post");
				Assert.assertEquals(post, pre);

				// We check this after checking comparison with 'post' for greater test result readibility
				Assert.assertTrue("We miss a transformation flag for: " + pre, transformed);
			}

			// Check the transformer is impact-less on already clean code
			// This is a less relevant test: to be done later
			{
				MethodDeclaration postPost = post.clone();
				Assert.assertFalse(transformer.walkNode(postPost));
				Assert.assertEquals(post, postPost);
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
				MethodDeclaration postPost = post.clone();
				Assert.assertFalse("Should not have mutated " + post + " but it turned into: " + postPost,
						transformer.walkNode(postPost));
				Assert.assertEquals(post, postPost);
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
					.filter(n -> n.getNameAsString().equals("Pre"))
					.findAny()
					.get();

			TypeDeclaration<?> post = oneCase.getMembers()
					.stream()
					.filter(n -> n instanceof TypeDeclaration)
					.map(n -> (TypeDeclaration<?>) n)
					.filter(n -> n.getNameAsString().equals("Post"))
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

	public Path getProjectTestSourceCode() throws IOException {
		Path srcMainResource = new ClassPathResource("empty").getFile().getParentFile().toPath();
		Assert.assertEquals("classes", srcMainResource.getName(srcMainResource.getNameCount() - 1).toString());
		Assert.assertEquals("target", srcMainResource.getName(srcMainResource.getNameCount() - 2).toString());
		Assert.assertEquals("java", srcMainResource.getName(srcMainResource.getNameCount() - 3).toString());
		return srcMainResource.resolve("./../../src/test/java").toAbsolutePath();
	}

	protected MethodDeclaration getMethodWithName(ClassOrInterfaceDeclaration oneCase, String name) {
		List<MethodDeclaration> preMethods = oneCase.getMethodsByName(name);
		if (preMethods.size() != 1) {
			throw new IllegalStateException("Expected a single '" + name + "' method in " + oneCase);
		}
		MethodDeclaration pre = preMethods.get(0);
		return pre;
	}
}
