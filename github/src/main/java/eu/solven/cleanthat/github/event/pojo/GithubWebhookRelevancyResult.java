package eu.solven.cleanthat.github.event.pojo;

import java.util.Optional;

public class GithubWebhookRelevancyResult implements IExternalWebhookRelevancyResult {
	final boolean prOpen;
	final boolean pushBranch;
	final boolean hasReviewRequest;
	final Optional<GitRepoBranchSha1> optRef;
	final Optional<GitPrHeadRef> optOpenPr;

	// This is a compatible base: trivial in case of PR event, implicit in case of commit_push (i.e. we take the base of
	// the first PR matching the head). We may prefer taking the latest matching PR (supposing older PR will not be
	// merged soon). However, it would mean a single head is being merged into different base: edge-case.
	final Optional<GitRepoBranchSha1> optBaseRef;

	public GithubWebhookRelevancyResult(boolean prOpen,
			boolean pushBranch,
			boolean hasReviewRequest,
			Optional<GitRepoBranchSha1> optRef,
			Optional<GitPrHeadRef> optOpenPr,
			Optional<GitRepoBranchSha1> optBaseRef) {
		this.prOpen = prOpen;
		this.pushBranch = pushBranch;
		this.hasReviewRequest = hasReviewRequest;
		this.optRef = optRef;
		this.optOpenPr = optOpenPr;
		this.optBaseRef = optBaseRef;
	}

	@Override
	public boolean isPrOpen() {
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
		return optRef;
	}

	public Optional<GitPrHeadRef> optOpenPr() {
		return optOpenPr;
	}

	public Optional<GitRepoBranchSha1> optBaseRef() {
		return optBaseRef;
	}
}
