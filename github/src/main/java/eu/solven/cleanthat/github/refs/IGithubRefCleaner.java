package eu.solven.cleanthat.github.refs;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.github.event.pojo.GitRepoBranchSha1;
import eu.solven.cleanthat.github.event.pojo.IExternalWebhookRelevancyResult;

/**
 * Holds the logic to clean a Ref (e.g. a PR, a Ref, a Branch, etc)
 *
 * @author Benoit Lacelle
 */
public interface IGithubRefCleaner {

	/**
	 * 
	 * @param offlineResult
	 * @param theRef
	 * @param relevantBaseBranches
	 * @return the ref to clean. Typically different to the input ref when we want to clean through a PR (e.g. not to
	 *         modify directly the dirty branch).
	 */
	Optional<String> prepareRefToClean(IExternalWebhookRelevancyResult offlineResult,
			GitRepoBranchSha1 theRef,
			Set<String> relevantBaseBranches);

	@Deprecated
	Map<String, ?> formatPR(Supplier<GHPullRequest> prSupplier);

	Map<String, ?> formatRef(GHRepository repo, Supplier<GHRef> refSupplier);

	Map<String, ?> formatRefDiff(GHRepository repo, Supplier<GHRef> baseSupplier, Supplier<GHRef> headSupplier);

}
