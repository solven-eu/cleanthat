package eu.solven.cleanthat.jgit;

/**
 * Helps knowing in which context a commit has been done
 *
 * @author Benoit Lacelle
 */
@Deprecated
public class CommitContext {

	final boolean commitOnMainBranch;

	final boolean branchWithoutPR;

	public CommitContext(boolean commitOnMainBranch, boolean branchWithoutPR) {
		this.commitOnMainBranch = commitOnMainBranch;
		this.branchWithoutPR = branchWithoutPR;
	}

	public boolean isCommitOnMainBranch() {
		return commitOnMainBranch;
	}

	public boolean isBranchWithoutPR() {
		return branchWithoutPR;
	}

	@Override
	public String toString() {
		return "CommitContext [commitOnMainBranch=" + commitOnMainBranch + ", branchWithoutPR=" + branchWithoutPR + "]";
	}
}
