package eu.solven.cleanthat.github;

/**
 * Constants related to Git standard
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitRefsConstants {

	// https://git-scm.com/book/en/v2/Git-Internals-Git-References
	// In a local Git repository, refs are available at 'refs/heads/XXX'
	String REFS_PREFIX = "refs/";
	String BRANCHES_PREFIX = REFS_PREFIX + "heads/";

	// https://stackoverflow.com/questions/1526471/git-difference-between-branchname-and-refs-heads-branchname
	String REF_REMOTES = REFS_PREFIX + "remotes/";
	String REF_TAGS = REFS_PREFIX + "tags/";

	String SHA1_CLEANTHAT_UP_TO_REF_ROOT = "CLEANTHAT_UP_TO_REF_ROOT";
}
