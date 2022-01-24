package eu.solven.cleanthat.config;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestIncludeExcludeHelpers {
	@Test
	public void testInvalidPath() {
		Assertions.assertThatThrownBy(() -> IncludeExcludeHelpers.prepareMatcher(Arrays.asList("notARegex(")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("notARegex");
	}
}
