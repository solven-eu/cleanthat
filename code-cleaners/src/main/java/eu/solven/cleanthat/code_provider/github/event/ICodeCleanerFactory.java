package eu.solven.cleanthat.code_provider.github.event;

import java.util.Optional;

import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;

/**
 * Enables returning an {@link IGitRefCleaner} for the target installation
 * 
 * @author Benoit Lacelle
 *
 */
public interface ICodeCleanerFactory {
	Optional<IGitRefCleaner> makeCleaner(Object somethingInteresting);
}
