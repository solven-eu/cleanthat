package io.cormoran.cleanthat.rules;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.rules.meta.IClassTransformer;

public class ATestCases {
	private static final Logger LOGGER = LoggerFactory.getLogger(ATestCases.class);

	protected void testCasesIn(Class<?> casesClass, IClassTransformer transformer) throws IOException {
		Path srcMainJava = getProjectTestSourceCode();
		// https://stackoverflow.com/questions/3190301/obtaining-java-source-code-from-class-name
		String path = casesClass.getName().replaceAll("\\.", "/") + ".java";
		CompilationUnit compilationUnit = StaticJavaParser.parse(srcMainJava.resolve(path));
		List<ClassOrInterfaceDeclaration> cases = compilationUnit.findAll(ClassOrInterfaceDeclaration.class, c -> {
			return !c.getMethodsByName("pre").isEmpty() && !c.getMethodsByName("post").isEmpty();
		});
		cases.forEach(oneCase -> {
			LOGGER.info("Processing the case: {}", oneCase.getName());
			MethodDeclaration pre = getMethodWithName(oneCase, "pre");
			MethodDeclaration post = getMethodWithName(oneCase, "post");

			// Check 'pre' is transformed into 'post'
			// This is generally the most relevant test: to be done first
			{
				transformer.transform(pre);
				// Rename the method bfore checking full equality
				pre.setName("post");
				Assert.assertEquals(post, pre);
			}

			// Check the transformer is impact-less on already clean code
			// This is a less relevant test: to be done later
			{
				MethodDeclaration postPost = post.clone();
				transformer.transform(postPost);
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
			throw new IllegalStateException("Expected a single 'pre' method in " + oneCase);
		}
		MethodDeclaration pre = preMethods.get(0);
		return pre;
	}
}
