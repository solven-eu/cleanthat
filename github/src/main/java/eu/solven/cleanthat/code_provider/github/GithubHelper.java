package eu.solven.cleanthat.code_provider.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;

/**
 * Helps working with Github
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubHelper.class);

	protected GithubHelper() {
		// hidden
	}

	public static GHBranch getDefaultBranch(GHRepository repo) {
		Set<String> branchNameCandidates = new LinkedHashSet<>();

		// https://stackoverflow.com/questions/16500461/how-do-i-find-the-default-branch-for-a-repository-using-the-github-v3-api
		String explicitDefaultBranch = repo.getDefaultBranch();
		if (!Strings.isNullOrEmpty(explicitDefaultBranch)) {
			branchNameCandidates.add(explicitDefaultBranch);
		}

		// It is unclear if default_branch is always provided or not
		branchNameCandidates.addAll(CleanthatRefFilterProperties.SIMPLE_DEFAULT_BRANCHES);

		Optional<GHBranch> optDefaultBranch = branchNameCandidates.stream().map(candidateBranch -> {
			GHBranch defaultBranch;
			try {
				defaultBranch = repo.getBranch(candidateBranch);
			} catch (GHFileNotFoundException e) {
				LOGGER.debug("There is no actual branch for name: " + candidateBranch, e);
				return Optional.<GHBranch>empty();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			return Optional.of(defaultBranch);
		}).flatMap(Optional::stream).findFirst();

		if (optDefaultBranch.isEmpty()) {
			Map<String, GHBranch> branches;
			try {
				branches = repo.getBranches();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			throw new IllegalStateException("Issue finding default branch. Explicit default=" + explicitDefaultBranch
					+ " Candidates="
					+ branchNameCandidates
					+ " branches="
					+ branches);
		}

		return optDefaultBranch.get();
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
