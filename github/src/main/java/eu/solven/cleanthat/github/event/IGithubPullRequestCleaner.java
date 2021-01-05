package eu.solven.cleanthat.github.event;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

/**
 * Holds the logic to clean a PR
 *
 * @author Benoit Lacelle
 */
public interface IGithubPullRequestCleaner {

	Optional<Map<String, ?>> branchConfig(GHBranch branch);

	Map<String, ?> formatPR(CommitContext commitContext, Supplier<GHPullRequest> pr);

	Map<String, ?> formatRef(CommitContext commitContext, GHRepository repo, Supplier<GHRef> refSupplier);
}
