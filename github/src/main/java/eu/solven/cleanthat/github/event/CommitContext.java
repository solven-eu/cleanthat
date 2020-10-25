package eu.solven.cleanthat.github.event;

/**
 * Helps knowing in which context a commit has been done
 * 
 * @author Benoit Lacelle
 *
 */
public class CommitContext {
	final boolean commitOnMainBranch;

	public CommitContext(boolean commitOnMainBranch) {
		this.commitOnMainBranch = commitOnMainBranch;
	}

	public boolean isCommitOnMainBranch() {
		return commitOnMainBranch;
	}

}
