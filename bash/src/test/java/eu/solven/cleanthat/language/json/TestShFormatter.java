package eu.solven.cleanthat.language.json;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.formatter.LineEnding;
import eu.solven.cleanthat.language.SourceCodeProperties;
import eu.solven.cleanthat.language.bash.beautysh.BeautyshFormatterProperties;
import eu.solven.cleanthat.language.bash.beautysh.BeautyshShFormatter;
import eu.solven.cleanthat.language.bash.beautysh.PepperResourceHelper;

@Ignore("TODO")
public class TestShFormatter {
	final ILintFixer formatter = new BeautyshShFormatter(new SourceCodeProperties(), new BeautyshFormatterProperties());

	@Test
	public void testBasicRaw() throws IOException {
		String formatted =
				formatter.doFormat(PepperResourceHelper.loadAsString("/do_not_format_me/basic_raw.sh"), LineEnding.LF);

		Assert.assertEquals("{ }", formatted);
	}
}
