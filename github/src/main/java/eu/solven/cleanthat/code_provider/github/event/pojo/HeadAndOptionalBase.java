package eu.solven.cleanthat.code_provider.github.event.pojo;

import java.util.Optional;

/**
 * This helps materializing a head to be cleaned, and an optional base. the base is missing when the head is directly
 * cleanable. The base might be a PR base, or a temporary branch to clean a branch without any PR.
 * 
 * @author Benoit Lacelle
 *
 */
public class HeadAndOptionalBase {
	// If present, it means the webhook is relevant and here we provide the essence of the commit (e.g. is this a push
	// to a ref, or a PR being open, or...)
	final GitRepoBranchSha1 headToClean;

	// If we have a base: it needs we should clean only the diff between the base and the branch
	final Optional<GitRepoBranchSha1> oBaseforHead;

	public HeadAndOptionalBase(GitRepoBranchSha1 headToClean, Optional<GitRepoBranchSha1> optBaseToConsider) {
		this.headToClean = headToClean;
		this.oBaseforHead = optBaseToConsider;
	}

	public GitRepoBranchSha1 getHeadToClean() {
		return headToClean;
	}

	public Optional<GitRepoBranchSha1> optBaseForHead() {
		return oBaseforHead;
	}
}
