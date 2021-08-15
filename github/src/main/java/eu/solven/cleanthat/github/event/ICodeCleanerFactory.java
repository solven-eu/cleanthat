package eu.solven.cleanthat.github.event;

import eu.solven.cleanthat.github.refs.IGithubRefCleaner;

/**
 * Enables returning an {@link IGithubRefCleaner} for the target installation
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeCleanerFactory {
	IGithubRefCleaner makeCleaner(Object somethingInteresting);
}
