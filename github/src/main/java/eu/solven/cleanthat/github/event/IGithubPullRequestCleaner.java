package eu.solven.cleanthat.github.event;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.github.GHPullRequest;

/**
 * Holds the logic to clean a PR
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubPullRequestCleaner {

	Map<String, ?> formatPR(Optional<Map<String, ?>> defaultBranchConfig,
			AtomicInteger nbBranchWithConfig,
			GHPullRequest pr);

}
