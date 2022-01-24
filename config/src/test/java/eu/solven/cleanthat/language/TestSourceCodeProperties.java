package eu.solven.cleanthat.language;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.formatter.LineEnding;

public class TestSourceCodeProperties {
	@Test
	public void testDefaultConstructor() {
		SourceCodeProperties properties = new SourceCodeProperties();

		// By default, neither LR or CRLF as we should not priviledge a plateform
		Assert.assertEquals(LineEnding.KEEP, properties.getLineEndingAsEnum());
	}
}
