package eu.solven.cleanthat.codeprovider.decorator;

import java.util.function.Supplier;

/**
 * Represents a Git repository
 * 
 * @author Benoit Lacelle
 *
 */
public interface ILazyGitReference extends IHasGitRef {
	/**
	 * 
	 * @param <T>
	 * @return the raw underlying item
	 */
	Supplier<IGitReference> getSupplier();
}
