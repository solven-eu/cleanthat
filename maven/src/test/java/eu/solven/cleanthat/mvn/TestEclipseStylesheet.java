package eu.solven.cleanthat.mvn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.language.java.eclipse.generator.EclipseStylesheetGenerator;

public class TestEclipseStylesheet {
	final EclipseStylesheetGenerator generator = new EclipseStylesheetGenerator();
	final CleanThatGenerateEclipseStylesheetMojo mojo = new CleanThatGenerateEclipseStylesheetMojo();

	{
		mojo.setJavaRegex(CleanThatGenerateEclipseStylesheetMojo.DEFAULT_JAVA_REGEX);
	}

	@Test
	public void testLoadFiles() {

		final String path = "./src/test/java/";

		Map<Path, String> contents = mojo.loadAnyJavaFile(Set.of(), generator, Set.of(Paths.get(path)));

		Assertions.assertThat(contents)
				.containsKey(Path.of("./src/test/java/eu/solven/cleanthat/mvn/TestEclipseStylesheet.java"));
	}

	// Typically on module with no 'src/main/java'
	@Test
	public void testLoadFiles_doesNotExist() {
		final String path = "./src/test/java/does_not_exists";

		Map<Path, String> contents = mojo.loadAnyJavaFile(Set.of(), generator, Set.of(Paths.get(path)));

		Assertions.assertThat(contents).isEmpty();
	}

	@Test
	public void testWriteSettings_exist() throws IOException {
		Path tmpPath = Files.createTempFile("cleanthat-TestEclipseStylesheet", ".xml");

		mojo.setConfigPath(tmpPath.toString());
		mojo.writeSettings(Map.of());
	}

	@Test
	public void testWriteSettings_doesNotExist_inSubFolder() throws IOException {
		Path tmpPath = Files.createTempDirectory("cleanthat-TestEclipseStylesheet");

		boolean deleted = tmpPath.toFile().delete();
		Assert.assertTrue(deleted);

		mojo.setConfigPath(tmpPath.resolve("eclipse-stylesheet.xml").toString());
		mojo.writeSettings(Map.of());
	}
}
