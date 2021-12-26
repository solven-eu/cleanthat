package eu.solven.cleanthat.mvn;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestCleanThatGenerateEclipseStylesheetMojo {
	CleanThatGenerateEclipseStylesheetMojo mojo = new CleanThatGenerateEclipseStylesheetMojo();

	@Test
	public void testNortmalizeEclipsePath() {
		Path normalized = mojo.normalize(
				Paths.get("/Users/blacelle/workspace2/RoaringBitmap/.cleanthat/eclipse_formatter-stylesheet.xml"),
				Paths.get("/Users/blacelle/workspace2/RoaringBitmap/cleanthat.yaml"));

		Assertions.assertThat(normalized.toString()).isEqualTo("/.cleanthat/eclipse_formatter-stylesheet.xml");
	}
}
