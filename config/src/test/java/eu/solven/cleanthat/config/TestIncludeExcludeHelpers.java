package eu.solven.cleanthat.config;

import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestIncludeExcludeHelpers {
	@Test
	public void testInvalidPath() {
		Assertions.assertThatThrownBy(() -> IncludeExcludeHelpers.prepareMatcher(Arrays.asList("notARegex(")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("notARegex");
	}

	@Test
	public void testAmbiguousDirectorySepqrqtor() {
		List<PathMatcher> pqthMqtchers =
				IncludeExcludeHelpers.prepareMatcher(Arrays.asList("regex:.*/do_not_format_me/.*"));

		// Under Windows: we would have a Windows PathMatcher, while we ensure the returned path holds '/' as directory
		// separator (e.g.
		// eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider.listFilesForContent(Consumer<ICodeProviderFile>))
		Optional<PathMatcher> optMatcher = IncludeExcludeHelpers.findMatching(pqthMqtchers,
				"/bash/src/main/resources/do_not_format_me/basic_raw.sh");

		Assertions.assertThat(optMatcher).isPresent();
	}
}
