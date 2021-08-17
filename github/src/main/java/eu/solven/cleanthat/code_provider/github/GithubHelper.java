package eu.solven.cleanthat.code_provider.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;

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
		// TODO master may not be the default default
		String defaultBranchName = Optional.ofNullable(repo.getDefaultBranch()).orElse("master");
		GHBranch defaultBranch;
		try {
			defaultBranch = repo.getBranch(defaultBranchName);
		} catch (GHFileNotFoundException e) {
			throw new IllegalStateException("We can not find default branch: " + defaultBranchName, e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return defaultBranch;
	}

	public static GHRef openEmptyRef(GHRepository repo, GHBranch base) {
		String cleanThatPrId = UUID.randomUUID().toString();
		String refName = GithubRefCleaner.PREFIX_REF_CLEANTHAT_MANUAL + cleanThatPrId;
		try {
			// https://docs.github.com/en/free-pro-team@latest/rest/reference/git#create-a-reference
			// If it doesn't start with 'refs' and have at least two slashes, it will be rejected.
			return repo.createRef(refName, base.getSHA1());
		} catch (IOException e) {
			throw new UncheckedIOException("Issue opening ref=" + refName, e);
		}
	}

	// Github does not allow opening a PR over a Ref matching the base (i.e. there must be at least one commit diff)
	public static GHPullRequest openPR(GHRepository repo, GHBranch base, GHRef ghRef) {
		// https://docs.github.com/rest/reference/pulls#create-a-pull-request
		try {
			return repo.createPullRequest("CleanThat - Cleaning style - ",
					ghRef.getRef(),
					base.getName(),
					"CleanThat cleaning PR",
					false,
					true);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue opening PR (" + ghRef + " -> " + base + ")", e);
		}
	}
}
