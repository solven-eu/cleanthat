package eu.solven.cleanthat.codeprovider.git;

import java.util.Optional;

/**
 * Holds details about a filter over an Event
 * 
 * @author Benoit Lacelle
 *
 */
public class GitWebhookRelevancyResult implements IExternalWebhookRelevancyResult {
	final boolean prOpen;
	final boolean pushBranch;
	// final boolean hasReviewRequest;
	final Optional<GitRepoBranchSha1> ref;
	final Optional<GitPrHeadRef> openPr;

	// This is a compatible base: trivial in case of PR event, implicit in case of commit_push (i.e. we take the base of
	// the first PR matching the head). We may prefer taking the latest matching PR (supposing older PR will not be
	// merged soon). However, it would mean a single head is being merged into different base: edge-case.
	final Optional<GitRepoBranchSha1> baseRef;

	public GitWebhookRelevancyResult(boolean prOpen,
			boolean pushBranch,
			// boolean hasReviewRequest,
			Optional<GitRepoBranchSha1> optRef,
			Optional<GitPrHeadRef> optOpenPr,
			Optional<GitRepoBranchSha1> optBaseRef) {
		this.prOpen = prOpen;
		this.pushBranch = pushBranch;
		// this.hasReviewRequest = hasReviewRequest;
		this.ref = optRef;
		this.openPr = optOpenPr;
		this.baseRef = optBaseRef;
	}

	@Override
	public boolean isReviewRequestOpen() {
		return prOpen;
	}

	@Override
	public boolean isPushBranch() {
		return pushBranch;
	}

	// @Override
	// public boolean refHasOpenReviewRequest() {
	// return hasReviewRequest;
	// }

	/**
	 * In case of a PR event, this holds the HEAD of the PR, not the base.
	 * 
	 * @return
	 */
	public Optional<GitRepoBranchSha1> optPushedRefOrRrHead() {
		return ref;
	}

	/**
	 * present only on PR event: when this POJO is built, we forbid ourselves scanning PRs, hence this can not be
	 * inferred on push events. This behavior may change if a CodeProvider enables push events, listing open PR for
	 * impacted branch in the original event.
	 * 
	 * @return
	 */
	public Optional<GitPrHeadRef> optOpenPr() {
		return openPr;
	}

	@Override
	public Optional<GitRepoBranchSha1> optBaseRef() {
		return baseRef;
	}
}
