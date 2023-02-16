package eu.solven.cleanthat.language.java.spotless;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.jimfs.Jimfs;

import eu.solven.cleanthat.code_provider.CleanthatPathHelpers;
import eu.solven.cleanthat.config.IncludeExcludeHelpers;
import eu.solven.cleanthat.spotless.language.JsonFormatterFactory;
import eu.solven.cleanthat.spotless.language.MarkdownFormatterFactory;
import eu.solven.cleanthat.spotless.language.PomXmlFormatterFactory;

public class TestSpotlessDefaultIncludes {
	final FileSystem fs = Jimfs.newFileSystem();
	final Path root = fs.getPath("TestSpotlessDefaultIncludes");

	@Test
	public void testDetectFiles_pomXml() {
		Set<String> includes = new PomXmlFormatterFactory().defaultIncludes();

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "pom.xml");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "directory/pom.xml");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "pre_pom.xml");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isEmpty();
		}
	}

	@Test
	public void testDetectFiles_markdown() {
		Set<String> includes = new MarkdownFormatterFactory().defaultIncludes();

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "README.MD");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "readme.md");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "directory/deep/README.md");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "CHANGES.md");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "CHANGES.narkdown");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isEmpty();
		}
	}

	@Test
	public void testDetectFiles_json() {
		Set<String> includes = new JsonFormatterFactory().defaultIncludes();

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "renovate.json");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "project/src/main/resources/config.json");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isPresent();
		}

		{
			Path filePath = CleanthatPathHelpers.makeContentPath(root, "main.js");

			List<PathMatcher> includeMatchers = IncludeExcludeHelpers.prepareMatcher(root.getFileSystem(), includes);
			Optional<PathMatcher> matchingInclude = IncludeExcludeHelpers.findMatching(includeMatchers, filePath);

			Assertions.assertThat(matchingInclude).isEmpty();
		}
	}
}
