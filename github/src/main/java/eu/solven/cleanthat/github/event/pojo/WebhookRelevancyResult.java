package eu.solven.cleanthat.github.event.pojo;

import java.util.Optional;

public class WebhookRelevancyResult {
	// If present, it means the webhook is relevant and here we provide the essence of the commit (e.g. is this a push
	// to a ref, or a PR being open, or...)
	// final Optional<CommitContext> optCommitContext;
	final Optional<String> optBranchToClean;

	// If we have a base: it needs we should clean only the diff between the base and the branch
	final Optional<GitRepoBranchSha1> optBaseToConsider;

	final Optional<String> optRejectedReason;

	public WebhookRelevancyResult(Optional<String> optBranchToClean,
			Optional<GitRepoBranchSha1> optBaseToConsider,
			Optional<String> rejectedReason) {
		this.optBranchToClean = optBranchToClean;
		this.optBaseToConsider = optBaseToConsider;
		this.optRejectedReason = rejectedReason;
	}

	public Optional<String> getOptBranchToClean() {
		return optBranchToClean;
	}

	public Optional<String> getOptRejectedReason() {
		return optRejectedReason;
	}

	public Optional<GitRepoBranchSha1> getOptBaseToConsider() {
		return optBaseToConsider;
	}

	public static WebhookRelevancyResult relevant(String branchToClean, Optional<GitRepoBranchSha1> optBase) {
		return new WebhookRelevancyResult(Optional.of(branchToClean), optBase, Optional.empty());
	}

	public static WebhookRelevancyResult dismissed(String reason) {
		return new WebhookRelevancyResult(Optional.empty(), Optional.empty(), Optional.of(reason));
	}
}
