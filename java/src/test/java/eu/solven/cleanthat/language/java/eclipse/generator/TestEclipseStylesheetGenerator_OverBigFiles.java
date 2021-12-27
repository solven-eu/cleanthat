package eu.solven.cleanthat.language.java.eclipse.generator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.formatter.linewrap.WrapPreparator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

// Beware this test is very slow: any performance improvment would be welcome
public class TestEclipseStylesheetGenerator_OverBigFiles {
	final EclipseStylesheetGenerator generator = new EclipseStylesheetGenerator();

	final Map<String, Map<String, String>> defaultConfigs = generator.loadDefaultConfigurations();

	final OffsetDateTime inOneHour = OffsetDateTime.now().plusHours(1);

	@Before
	public void before() {
		// https://stackoverflow.com/questions/28901375/how-do-i-disable-java-assertions-for-a-junit-test-in-the-code
		// We encounter an unexpected assertion
		// TODO Report the issue to eclipse
		WrapPreparator.class.getClassLoader().setClassAssertionStatus(WrapPreparator.class.getName(), false);
	}

	@Test
	public void testRoaringBitmap() throws IOException {
		Resource testRoaringBitmapSource = new ClassPathResource("/source/RoaringBitmap/RoaringBitmap.java");
		String asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		Map<Path, String> pathToFile = Map.of(Paths.get("/RoaringBitmap.java"), asString);

		ScoredOption<Map<String, String>> defaultConf = generator.findBestDefaultSetting(inOneHour, pathToFile);

		// Check we consider eclipse java is the best default configuration
		{
			Assertions.assertThat(defaultConfigs.entrySet()
					.stream()
					.filter(e -> e.getValue().equals(defaultConf.getOption()))
					.map(e -> e.getKey())
					.findAny()
					.get()).isEqualTo("google");
		}

		{
			Map<String, String> settings =
					generator
							.optimizeSetOfSettings(inOneHour,
									pathToFile,
									defaultConf,
									Set.of("org.eclipse.jdt.core.formatter.tabulation.char",
											"org.eclipse.jdt.core.formatter.tabulation.size"))
							.getOption();

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "2")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "100");
		}

		// All settings
		// TODO Very slow
		if (false) {
			Map<String, String> settings = generator.generateSettings(inOneHour, pathToFile);

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "2")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "240");
		}
	}

	@Test
	public void testTestRoaringBitmap() throws IOException {
		Resource testRoaringBitmapSource = new ClassPathResource("/source/RoaringBitmap/TestRoaringBitmap.java");
		String asString =
				new String(ByteStreams.toByteArray(testRoaringBitmapSource.getInputStream()), StandardCharsets.UTF_8);

		Map<Path, String> pathToFile = Map.of(Paths.get("/TestRoaringBitmap.java"), asString);

		ScoredOption<Map<String, String>> defaultConf = generator.findBestDefaultSetting(inOneHour, pathToFile);

		// Check we consider eclipse java is the best default configuration
		{
			Assertions.assertThat(defaultConfigs.entrySet()
					.stream()
					.filter(e -> e.getValue().equals(defaultConf.getOption()))
					.map(e -> e.getKey())
					.findAny()
					.get()).isEqualTo("default");
		}

		{
			Map<String, String> settings =
					generator
							.optimizeSetOfSettings(inOneHour,
									pathToFile,
									defaultConf,
									Set.of("org.eclipse.jdt.core.formatter.tabulation.char",
											"org.eclipse.jdt.core.formatter.tabulation.size"))
							.getOption();

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "4")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "120");
		}

		// All settings
		// TODO Very slow
		if (false) {
			Map<String, String> settings = generator.generateSettings(inOneHour, pathToFile);

			Assertions.assertThat(settings)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE)
					.containsEntry("org.eclipse.jdt.core.formatter.tabulation.size", "2")
					.containsEntry("org.eclipse.jdt.core.formatter.lineSplit", "240");
		}
	}
}
