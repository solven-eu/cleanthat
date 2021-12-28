package eu.solven.cleanthat.code_provider.github.event;

import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;

/**
 * Enables returning an {@link IGitRefCleaner} for the target installation
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeCleanerFactory {
	IGitRefCleaner makeCleaner(Object somethingInteresting);
}
