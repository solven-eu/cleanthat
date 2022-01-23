package eu.solven.cleanthat.codeprovider.decorator;

/**
 * Represents a Git commit. i.e. a pointer with a specific sha1
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitCommit {
	/**
	 * 
	 * @param <T>
	 * @return the raw underlying item
	 */
	<T> T getDecorated();
}
