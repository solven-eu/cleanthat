package eu.solven.cleanthat.code_provider.github.refs;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.event.pojo.GitRepoBranchSha1;
import eu.solven.cleanthat.code_provider.github.event.pojo.HeadAndOptionalBase;
import eu.solven.cleanthat.code_provider.github.event.pojo.IExternalWebhookRelevancyResult;
import eu.solven.cleanthat.formatter.CodeFormatResult;

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
	Optional<HeadAndOptionalBase> prepareRefToClean(IExternalWebhookRelevancyResult offlineResult,
			GitRepoBranchSha1 theRef,
			Set<String> relevantBaseBranches);

	CodeFormatResult formatRef(GHRepository repo, Supplier<GHRef> refSupplier);

	CodeFormatResult formatRefDiff(GHRepository repo, GHRef baseSupplier, Supplier<GHRef> headSupplier);

}
