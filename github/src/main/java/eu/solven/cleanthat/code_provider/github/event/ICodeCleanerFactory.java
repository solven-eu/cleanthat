package eu.solven.cleanthat.code_provider.github.event;

import eu.solven.cleanthat.code_provider.github.refs.IGithubRefCleaner;

/**
 * Enables returning an {@link IGithubRefCleaner} for the target installation
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeCleanerFactory {
	IGithubRefCleaner makeCleaner(Object somethingInteresting);
}
