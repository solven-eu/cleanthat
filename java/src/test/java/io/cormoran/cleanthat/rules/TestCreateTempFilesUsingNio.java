package io.cormoran.cleanthat.rules;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.rules.CreateTempFilesUsingNio;
import eu.solven.cleanthat.rules.cases.CreateTempFilesUsingNioCases;
import eu.solven.cleanthat.rules.meta.IClassTransformer;
import eu.solven.cleanthat.rules.test.ATestCases;

public class TestCreateTempFilesUsingNio extends ATestCases {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestCreateTempFilesUsingNio.class);

	@Test
	public void testCases() throws IOException {
		testCasesIn(CreateTempFilesUsingNioCases.class, new CreateTempFilesUsingNio());
	}

	@Test
	public void testImportNioFiles() throws IOException {
		Path srcMainJava = getProjectTestSourceCode();
		// https://stackoverflow.com/questions/3190301/obtaining-java-source-code-from-class-name
		String path = CreateTempFilesUsingNioCases.class.getName().replaceAll("\\.", "/") + ".java";

		JavaParser javaParser = RulesJavaMutator.makeDefaultJavaParser();
		CompilationUnit compilationUnit = javaParser.parse(srcMainJava.resolve(path)).getResult().get();

		List<ClassOrInterfaceDeclaration> cases = compilationUnit.findAll(ClassOrInterfaceDeclaration.class, c -> {
			return !c.getMethodsByName("pre").isEmpty() && !c.getMethodsByName("post").isEmpty();
		});
		IClassTransformer transformer = new CreateTempFilesUsingNio();
		cases.forEach(oneCase -> {
			LOGGER.info("Processing the case: {}", oneCase.getName());
			MethodDeclaration pre = getMethodWithName(oneCase, "pre");
			MethodDeclaration post = getMethodWithName(oneCase, "post");
			// Check if 'pre' transformation into 'post' add the good library
			// TODO not yet testing import of Paths
			{
				compilationUnit.getImports()
						.removeIf(im -> new ImportDeclaration("java.nio.file.Files", false, false).equals(im));
				transformer.walkNode(pre);
				// Rename the method before checking full equality
				pre.setName("post");
				Assert.assertTrue(compilationUnit.getImports()
						.contains(new ImportDeclaration("java.nio.file.Files", false, false)));
			}
		});
	}
}
