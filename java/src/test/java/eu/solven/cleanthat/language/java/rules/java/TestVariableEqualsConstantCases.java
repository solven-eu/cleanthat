package eu.solven.cleanthat.language.java.rules.java;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.google.common.io.ByteStreams;

import eu.solven.cleanthat.java.mutators.RulesJavaMutator;
import eu.solven.cleanthat.language.java.rules.cases.do_not_format_me.VariableEqualsConstantCases;
import eu.solven.cleanthat.language.java.rules.meta.IClassTransformer;
import eu.solven.cleanthat.language.java.rules.mutators.VariableEqualsConstant;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;

public class TestVariableEqualsConstantCases extends ATestCases {

	@Test
	public void testCases() throws IOException {
		testCasesIn(new VariableEqualsConstantCases());
	}

	// Cannot keep element because we reached the end of nodetext
	@Test
	public void testIssueWithFile() throws IOException {
		Resource testRoaringBitmapSource =
				new ClassPathResource("/source/do_not_format_me/MiTrust/TestNodeResourceImpl.java");
		String asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		IClassTransformer transformer = new VariableEqualsConstant();
		JavaParser javaParser = RulesJavaMutator.makeDefaultJavaParser(transformer.isJreOnly());
		CompilationUnit compilationUnit = javaParser.parse(asString).getResult().get();

		boolean transformed = transformer.walkNode(compilationUnit);

		Assertions.assertThat(transformed).isTrue();
	}
}
