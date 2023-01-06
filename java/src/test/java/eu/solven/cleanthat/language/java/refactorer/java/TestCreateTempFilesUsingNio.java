package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;
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

import eu.solven.cleanthat.language.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.CreateTempFilesUsingNioCases;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.CreateTempFilesUsingNio;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;
import eu.solven.cleanthat.language.java.refactorer.test.LocalClassTestHelper;

public class TestCreateTempFilesUsingNio extends ATestCases {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestCreateTempFilesUsingNio.class);

	final CreateTempFilesUsingNio transformer = new CreateTempFilesUsingNio();

	@Test
	public void testCases() throws IOException {
		testCasesIn(CreateTempFilesUsingNioCases.class, transformer);
	}

	@Test
	public void testImportNioFiles() throws IOException {
		JavaParser javaParser = JavaRefactorer.makeDefaultJavaParser(transformer.isJreOnly());
		CompilationUnit compilationUnit =
				javaParser.parse(LocalClassTestHelper.localClassAsPath(CreateTempFilesUsingNioCases.class))
						.getResult()
						.get();

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
