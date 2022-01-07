package eu.solven.cleanthat.language.xml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.xml.javax.JavaxXmlFormatter;
import eu.solven.cleanthat.language.xml.javax.JavaxXmlFormatterProperties;

public class TestJavaxXmlFormatter {
	final ILintFixer formatter = new JavaxXmlFormatter(new SourceCodeProperties(), new JavaxXmlFormatterProperties());

	@Test
	public void testFormatNote_lf() throws IOException {
		String expectedXml = StreamUtils.copyToString(new ClassPathResource("/xml/note.xml").getInputStream(),
				StandardCharsets.UTF_8);

		String formatted = formatter.doFormat(expectedXml, LineEnding.LF);

		// We would need to rely on LSSerializer to force the EOL
		Assertions.assertThat(formatted.split(System.lineSeparator()))
				.hasSize(7)
				// This is not satisfying to see standalone added explicitly
				.contains("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")
				// This is not satisfying as comment and first first node are on the same line
				.contains("<!-- view-source:https://www.w3schools.com/xml/note.xml --><note>");
	}

	@Test
	public void testFormatNote_crlf() throws IOException {
		String expectedXml = StreamUtils.copyToString(new ClassPathResource("/xml/note.xml").getInputStream(),
				StandardCharsets.UTF_8);

		String formatted = formatter.doFormat(expectedXml, LineEnding.CRLF);

		// We would need to rely on LSSerializer to force the EOL
		Assertions.assertThat(formatted.split(System.lineSeparator()))
				.hasSize(7)
				// This is not satisfying to see standalone added explicitly
				.contains("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")
				// This is not satisfying as comment and first first node are on the same line
				.contains("<!-- view-source:https://www.w3schools.com/xml/note.xml --><note>");
	}
}
