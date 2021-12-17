package eu.solven.cleanthat.language.java.eclipse.generator;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestEclipseStylesheetGenerator {
	final EclipseStylesheetGenerator generator = new EclipseStylesheetGenerator();

	@Test
	public void testDeltaDiff() {
		Assertions.assertThat(generator.deltaDiff("abcd", "efgh")).isEqualTo(4);

		Assertions.assertThat(generator.deltaDiff("abcd", "_abcd_")).isEqualTo(2);
	}
}
