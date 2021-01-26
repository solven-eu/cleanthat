package eu.solven.cleanthat.github.event;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.jgit.CommitContext;

/**
 * Holds the logic to clean a PR
 *
 * @author Benoit Lacelle
 */
public interface IGithubPullRequestCleaner {

	Optional<Map<String, ?>> branchConfig(GHBranch branch);

	Map<String, ?> formatPR(String token, CommitContext commitContext, Supplier<GHPullRequest> prSupplier);

	Map<String, ?> formatRef(String token, CommitContext commitContext, GHRepository repo, Supplier<GHRef> refSupplier);

}
