package eu.solven.cleanthat.language.xml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatter;
import eu.solven.cleanthat.language.xml.revelc.RevelcXmlFormatterProperties;

public class TestRevelcXmlFormatter {

	final ILintFixer formatter = new RevelcXmlFormatter(new SourceCodeProperties(), new RevelcXmlFormatterProperties());

	@Test
	public void testFormatNote_lf() throws IOException {
		String expectedXml =
				StreamUtils.copyToString(new ClassPathResource("/do_not_format_me/xml/note.xml").getInputStream(),
						StandardCharsets.UTF_8);
		LineEnding eol = LineEnding.LF;
		String formatted = formatter.doFormat(expectedXml, eol);
		// TODO Investigate why it is not expected EOL by system.eol which is applied
		Assertions.assertThat(formatted.split(eol.optChars().orElseThrow()))
				.hasSize(8)
				.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.contains("<!-- view-source:https://www.w3schools.com/xml/note.xml -->");
	}

	@Test
	public void testFormatNote_crlf() throws IOException {
		// TODO This tests is OK only under Windows
		Assume.assumeTrue("\r\n".equals(System.lineSeparator()));
		String expectedXml =
				StreamUtils.copyToString(new ClassPathResource("/do_not_format_me/xml/note.xml").getInputStream(),
						StandardCharsets.UTF_8);
		LineEnding eol = LineEnding.CRLF;
		String formatted = formatter.doFormat(expectedXml, eol);
		// TODO Investigate why it is not expected EOL by system.eol which is applied
		Assertions.assertThat(formatted.split(eol.optChars().orElseThrow()))
				.hasSize(8)
				.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.contains("<!-- view-source:https://www.w3schools.com/xml/note.xml -->");
	}
}
