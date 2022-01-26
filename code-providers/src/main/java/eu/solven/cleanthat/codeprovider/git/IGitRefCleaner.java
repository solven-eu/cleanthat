package eu.solven.cleanthat.codeprovider.git;

import java.util.Optional;
import java.util.Set;

import eu.solven.cleanthat.codeprovider.decorator.IGitBranch;
import eu.solven.cleanthat.codeprovider.decorator.IGitCommit;
import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.decorator.IGitRepository;
import eu.solven.cleanthat.codeprovider.decorator.ILazyGitReference;
import eu.solven.cleanthat.formatter.CodeFormatResult;

/**
 * Holds the logic to clean a Ref (e.g. a PR, a Ref, a Branch, etc)
 *
 * @author Benoit Lacelle
 */
public interface IGitRefCleaner {

	boolean tryOpenPRWithCleanThatStandardConfiguration(IGitBranch branch);

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

	/**
	 * Format a whole ref, moving its head to a cleaned commit
	 * 
	 * @param repo
	 * @param refSupplier
	 * @return
	 */
	CodeFormatResult formatRef(IGitRepository repo, IGitBranch branchSupplier, ILazyGitReference headSupplier);

	@Deprecated
	CodeFormatResult formatRefDiff(IGitRepository repo, IGitReference baseSupplier, ILazyGitReference headSupplier);

	/**
	 * Format a ref, based on its diff with a base commit
	 * 
	 * @param repo
	 * @param baseSupplier
	 * @param headSupplier
	 * @return
	 */
	CodeFormatResult formatCommitToRefDiff(IGitRepository repo,
			IGitCommit baseSupplier,
			ILazyGitReference headSupplier);

}
