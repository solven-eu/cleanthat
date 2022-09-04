package eu.solven.cleanthat.formatter;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestLineEnding {
	@Test
	public void testGuessEolEmptyFile() {
		Assertions.assertThat(LineEnding.getOrGuess(LineEnding.KEEP, () -> "")).isEqualTo(System.lineSeparator());

		Assertions.assertThat(LineEnding.getOrGuess(LineEnding.KEEP, () -> "a\rb\rc")).isEqualTo("\r");
		Assertions.assertThat(LineEnding.getOrGuess(LineEnding.KEEP, () -> "a\r\nb\r\nc")).isEqualTo("\r\n");
	}
}
