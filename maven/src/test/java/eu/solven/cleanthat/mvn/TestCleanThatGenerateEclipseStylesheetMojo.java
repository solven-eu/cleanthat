package eu.solven.cleanthat.mvn;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.SystemUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
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

	@Test
	public void testNortmalizeEclipsePath_windows() {
		Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
		Path normalized = mojo.normalize(
				Paths.get(
						"C:\\Users\\blacelle\\workspace2\\RoaringBitmap\\.cleanthat\\eclipse_formatter-stylesheet.xml"),
				Paths.get("C:\\Users\\blacelle\\workspace2\\RoaringBitmap\\cleanthat.yaml"));

		Assertions.assertThat(normalized.toString()).isEqualTo("/.cleanthat/eclipse_formatter-stylesheet.xml");
	}
}
