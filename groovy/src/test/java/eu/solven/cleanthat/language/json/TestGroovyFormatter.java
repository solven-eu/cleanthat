package eu.solven.cleanthat.language.json;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.groovy.jackson.EclipseGroovyFormatter;
import eu.solven.cleanthat.language.groovy.jackson.EclipseGroovyFormatterProperties;

public class TestGroovyFormatter {
	final ILintFixer formatter =
			new EclipseGroovyFormatter(new SourceCodeProperties(), new EclipseGroovyFormatterProperties());

	@Test
	public void testFormatJson() throws IOException {
		String formatted = formatter.doFormat("{\r}", LineEnding.LF);

		Assert.assertEquals("{ }", formatted);
	}
}
