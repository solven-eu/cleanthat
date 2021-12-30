package eu.solven.cleanthat.codeprovider.decorator;

/**
 * Represents a Git branch. i.e. a named ref which is not a tag.
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitBranch {
	/**
	 * 
	 * @param <T>
	 * @return the raw underlying item
	 */
	<T> T getDecorated();
}
