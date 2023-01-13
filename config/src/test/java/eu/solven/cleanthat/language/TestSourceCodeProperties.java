package eu.solven.cleanthat.language;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.config.pojo.SourceCodeProperties;
import eu.solven.cleanthat.formatter.LineEnding;

public class TestSourceCodeProperties {
	@Test
	public void testDefaultConstructor() {
		SourceCodeProperties properties = new SourceCodeProperties();

		// By default, neither LR or CRLF as we should not privilege a platform
		// We rely on UNKNOWN so that any other parameter takes precedence
		Assert.assertEquals(LineEnding.UNKNOWN, properties.getLineEndingAsEnum());
	}
}
