package eu.solven.cleanthat.java.spring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.formatter.IStyleEnforcer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.java.mutators.JavaparserDirtyMe;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.java.rules.test.ATestCases;
import eu.solven.cleanthat.language.java.spring.SpringJavaFormatterProperties;
import eu.solven.cleanthat.language.java.spring.SpringJavaStyleEnforcer;

public class TestSpringStyleEnforcer {
	@Ignore("TODO")
	@Test
	public void testCleanJavaparserUnexpectedChanges() throws IOException {
		Path srcMainJava = ATestCases.getProjectTestSourceCode();
		// https://stackoverflow.com/questions/3190301/obtaining-java-source-code-from-class-name
		String path = JavaparserDirtyMe.class.getName().replaceAll("\\.", "/") + ".java";

		Path pathToDirty = srcMainJava.resolve(path);

		String dirtyCode = Files.readString(pathToDirty);

		// We need any styleEnforce to workaround Javaparsser weaknesses
		IStyleEnforcer styleEnforcer =
				new SpringJavaStyleEnforcer(new SourceCodeProperties(), new SpringJavaFormatterProperties());

		LineEnding lineEnding = LineEnding.determineLineEnding(dirtyCode);
		styleEnforcer.doFormat(dirtyCode, lineEnding);
	}
}
