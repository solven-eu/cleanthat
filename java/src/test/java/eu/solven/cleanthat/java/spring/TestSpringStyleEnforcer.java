package eu.solven.cleanthat.java.spring;

import java.io.IOException;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.java.refactorer.JavaparserDirtyMe;
import eu.solven.cleanthat.language.java.refactorer.test.LocalClassTestHelper;
import eu.solven.cleanthat.language.java.spring.SpringJavaFormatterProperties;
import eu.solven.cleanthat.language.java.spring.SpringJavaStyleEnforcer;

public class TestSpringStyleEnforcer {
	@Ignore("TODO")
	@Test
	public void testCleanJavaparserUnexpectedChanges() throws IOException {
		String dirtyCode = LocalClassTestHelper.loadClassAsString(JavaparserDirtyMe.class);

		// We need any styleEnforce to workaround Javaparser weaknesses
		IStyleEnforcer styleEnforcer =
				new SpringJavaStyleEnforcer(new SourceCodeProperties(), new SpringJavaFormatterProperties());

		Optional<LineEnding> lineEnding = LineEnding.determineLineEnding(dirtyCode);
		Assertions.assertThat(lineEnding).isPresent();
		styleEnforcer.doFormat(dirtyCode, lineEnding.get());
	}
}
