package eu.solven.cleanthat.language.json;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatter;
import eu.solven.cleanthat.language.json.jackson.JacksonJsonFormatterProperties;

public class TestJsonFormatter {
	final ILintFixer formatter =
			new JacksonJsonFormatter(new SourceCodeProperties(), new JacksonJsonFormatterProperties());

	@Test
	public void testFormatJson() throws IOException {
		String formatted = formatter.doFormat("{\r}", LineEnding.LF);

		Assert.assertEquals("{ }", formatted);
	}
}
