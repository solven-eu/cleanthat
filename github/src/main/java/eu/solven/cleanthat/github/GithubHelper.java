package eu.solven.cleanthat.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;

/**
 * Helps working with Github
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubHelper {
	protected GithubHelper() {
		// hidden
	}

	public static GHBranch getDefaultBranch(GHRepository repo) {
		String defaultBranchName = Optional.ofNullable(repo.getDefaultBranch()).orElse("master");
		GHBranch defaultBranch;
		try {
			defaultBranch = repo.getBranch(defaultBranchName);
		} catch (GHFileNotFoundException e) {
			throw new IllegalStateException("We can not find as default branch: " + defaultBranchName, e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return defaultBranch;
	}
}
