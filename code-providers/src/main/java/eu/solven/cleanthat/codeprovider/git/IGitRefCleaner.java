package eu.solven.cleanthat.codeprovider.git;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.decorator.IGitRepository;
import eu.solven.cleanthat.formatter.CodeFormatResult;

/**
 * Holds the logic to clean a Ref (e.g. a PR, a Ref, a Branch, etc)
 *
 * @author Benoit Lacelle
 */
public interface IGitRefCleaner {

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

	CodeFormatResult formatRef(IGitRepository repo, Supplier<IGitReference> refSupplier);

	CodeFormatResult formatRefDiff(IGitRepository repo,
			IGitReference baseSupplier,
			Supplier<IGitReference> headSupplier);

}
