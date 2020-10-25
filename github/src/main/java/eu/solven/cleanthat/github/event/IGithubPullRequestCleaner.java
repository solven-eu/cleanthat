package eu.solven.cleanthat.github.event;

import java.util.Map;
import java.util.Optional;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHPullRequest;

/**
 * Holds the logic to clean a PR
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubPullRequestCleaner {

	Map<String, ?> formatPR(CommitContext commitContext, GHPullRequest pr);

	Optional<Map<String, ?>> branchConfig(GHBranch branch);

}
