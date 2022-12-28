package eu.solven.cleanthat.config;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helpers related to include and exclude rules
 * 
 * @author Benoit Lacelle
 *
 */
public class IncludeExcludeHelpers {
	// It is good to know that '/' will be interpreted as folder separator even under Windows
	// https://stackoverflow.com/questions/9148528/how-do-i-use-directory-globbing-in-jdk7
	public static final List<String> DEFAULT_INCLUDES_JAVA = Arrays.asList("glob:**/*.java");

	protected IncludeExcludeHelpers() {
		// hidden
	}

	// https://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
	public static Optional<PathMatcher> findMatching(List<PathMatcher> includeMatchers, String fileName) {
		return includeMatchers.stream().filter(pm -> pm.matches(Paths.get(fileName))).findFirst();
	}

	// https://stackoverflow.com/questions/44388227/sonar-raises-blocker-issue-on-java-filesystems-getdefault
	@SuppressWarnings("PMD.CloseResource")
	public static List<PathMatcher> prepareMatcher(List<String> regex) {
		FileSystem fs = FileSystems.getDefault();
		return regex.stream().map(r -> {
			try {
				String newPattern;
				// https://stackoverflow.com/questions/64102053/java-pathmatcher-not-working-properly-on-windows
				if (File.separator.equals("\\")) {
					// We are under Windows
					newPattern = r.replace("/", "\\\\");
				} else {
					// We are under Linux
					newPattern = r;
				}

				return fs.getPathMatcher(newPattern);
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Invalid regex: " + r, e);
			}
		}).collect(Collectors.toList());
	}
}
