package eu.solven.cleanthat.language.xml;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.xml.jackson.JacksonXmlFormatter;
import eu.solven.cleanthat.language.xml.jackson.JacksonXmlFormatterProperties;

public class TestXmlFormatter {
	final ILintFixer formatter =
			new JacksonXmlFormatter(new SourceCodeProperties(), new JacksonXmlFormatterProperties());

	@Test
	public void testFormatJson() throws IOException {
		String formatted = formatter.doFormat("{\r}", LineEnding.LF);

		Assert.assertEquals("{ }", formatted);
	}
}
