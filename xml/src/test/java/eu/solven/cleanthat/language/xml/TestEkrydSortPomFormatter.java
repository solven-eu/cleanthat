package eu.solven.cleanthat.language.xml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Index;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import eu.solven.cleanthat.formatter.ICommonConventions;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.xml.ekryd_sortpom.EcrydSortPomFormatter;
import eu.solven.cleanthat.language.xml.ekryd_sortpom.EcrydSortPomFormatterProperties;

public class TestEkrydSortPomFormatter {
	final EcrydSortPomFormatterProperties properties = new EcrydSortPomFormatterProperties();

	final ILintFixer formatter = new EcrydSortPomFormatter(new SourceCodeProperties(), properties);

	@Test
	public void testSort_defaultIndentation() throws IOException {
		String dirty = StreamUtils.copyToString(new ClassPathResource("/pom/mis_ordered.xml").getInputStream(),
				StandardCharsets.UTF_8);

		Assertions.assertThat(dirty.split(System.lineSeparator()))
				.hasSize(23)

				.contains("\t<dependencies>", Index.atIndex(5))
				.contains("\t\t\t<version>0.2.2</version>", Index.atIndex(7))

				.contains("\t<!-- SomeComment on artifactId -->", Index.atIndex(13))
				.contains("\t<artifactId>xml</artifactId>", Index.atIndex(14))

				.contains("\t<parent>", Index.atIndex(16));

		String clean = formatter.doFormat(dirty, LineEnding.determineLineEnding(dirty).orElseThrow());

		Assertions.assertThat(clean.split(System.lineSeparator()))
				.hasSize(22)

				.contains("    <parent>", Index.atIndex(4))

				.contains("    <!-- SomeComment on artifactId -->", Index.atIndex(10))
				.contains("    <artifactId>xml</artifactId>", Index.atIndex(11))

				.contains("    <dependencies>", Index.atIndex(13))
				.contains("            <version>0.2.2</version>", Index.atIndex(17));
	}

	@Test
	public void testSort_tabIndentation() throws IOException {
		properties.setIndent(ICommonConventions.DEFAULT_INDENT_FOR_TAB);

		String dirty = StreamUtils.copyToString(new ClassPathResource("/pom/mis_ordered.xml").getInputStream(),
				StandardCharsets.UTF_8);

		Assertions.assertThat(dirty.split(System.lineSeparator()))
				.hasSize(23)

				.contains("\t<dependencies>", Index.atIndex(5))
				.contains("\t\t\t<version>0.2.2</version>", Index.atIndex(7))

				.contains("\t<!-- SomeComment on artifactId -->", Index.atIndex(13))
				.contains("\t<artifactId>xml</artifactId>", Index.atIndex(14))

				.contains("\t<parent>", Index.atIndex(16));

		String clean = formatter.doFormat(dirty, LineEnding.determineLineEnding(dirty).orElseThrow());

		Assertions.assertThat(clean.split(System.lineSeparator()))
				.hasSize(22)

				.contains("\t<parent>", Index.atIndex(4))

				.contains("\t<!-- SomeComment on artifactId -->", Index.atIndex(10))
				.contains("\t<artifactId>xml</artifactId>", Index.atIndex(11))

				.contains("\t<dependencies>", Index.atIndex(13))
				.contains("\t\t\t<version>0.2.2</version>", Index.atIndex(17));
	}
}
