package eu.solven.cleanthat.code_provider.github.event.pojo;

import java.util.Optional;

/**
 * Holds details about a filter over an Event
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubWebhookRelevancyResult implements IExternalWebhookRelevancyResult {
	final boolean prOpen;
	final boolean pushBranch;
	final boolean hasReviewRequest;
	final Optional<GitRepoBranchSha1> ref;
	final Optional<GitPrHeadRef> openPr;

	// This is a compatible base: trivial in case of PR event, implicit in case of commit_push (i.e. we take the base of
	// the first PR matching the head). We may prefer taking the latest matching PR (supposing older PR will not be
	// merged soon). However, it would mean a single head is being merged into different base: edge-case.
	final Optional<GitRepoBranchSha1> baseRef;

	public GithubWebhookRelevancyResult(boolean prOpen,
			boolean pushBranch,
			boolean hasReviewRequest,
			Optional<GitRepoBranchSha1> optRef,
			Optional<GitPrHeadRef> optOpenPr,
			Optional<GitRepoBranchSha1> optBaseRef) {
		this.prOpen = prOpen;
		this.pushBranch = pushBranch;
		this.hasReviewRequest = hasReviewRequest;
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

	@Override
	public boolean refHasOpenReviewRequest() {
		return hasReviewRequest;
	}

	public Optional<GitRepoBranchSha1> optPushedRef() {
		return ref;
	}

	public Optional<GitPrHeadRef> optOpenPr() {
		return openPr;
	}

	@Override
	public Optional<GitRepoBranchSha1> optBaseRef() {
		return baseRef;
	}
}
