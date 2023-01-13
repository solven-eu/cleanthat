package eu.solven.cleanthat.language.java.refactorer.java;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.language.java.refactorer.JavaRefactorer;
import eu.solven.cleanthat.language.java.refactorer.cases.do_not_format_me.LiteralsFirstInComparisonsCases;
import eu.solven.cleanthat.language.java.refactorer.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.refactorer.mutators.LiteralsFirstInComparisons;
import eu.solven.cleanthat.language.java.refactorer.test.ATestCases;

public class TestLiteralsFirstInComparisonsCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new LiteralsFirstInComparisonsCases());
	}

	// Cannot keep element because we reached the end of nodetext
	@Test
	public void testIssueWithFile() throws IOException {
		Resource testRoaringBitmapSource =
				new ClassPathResource("/source/do_not_format_me/MiTrust/TestNodeResourceImpl.java");
		String asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		IClassTransformer transformer = new LiteralsFirstInComparisons();
		JavaParser javaParser = JavaRefactorer.makeDefaultJavaParser(transformer.isJreOnly());
		CompilationUnit compilationUnit = javaParser.parse(asString).getResult().get();

		boolean transformed = transformer.walkNode(compilationUnit);

		Assertions.assertThat(transformed).isTrue();
	}
}
