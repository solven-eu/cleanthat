package eu.solven.cleanthat.codeprovider.decorator;

/**
 * Represents a Git reference
 * 
 * @author Benoit Lacelle
 *
 */
public interface IHasGitRef {
	/**
	 * Typically refs/heads/XXX for a branch named XXX
	 * 
	 * @return
	 */
	String getFullRefOrSha1();
}
