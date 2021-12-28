package eu.solven.cleanthat.codeprovider.decorator;

/**
 * Represents a Git repository
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitReference {
	/**
	 * 
	 * @param <T>
	 * @return the raw underlying item
	 */
	<T> T getDecorated();
}
