package eu.solven.cleanthat.code_provider.github.event;

import java.util.List;
import java.util.Optional;

import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;

/**
 * Factory for {@link IGitRefCleaner}
 * 
 * @author Benoit Lacelle
 *
 */
public class CompositeCodeCleanerFactory implements ICodeCleanerFactory {

	final List<ICodeCleanerFactory> specializedFactories;

	public CompositeCodeCleanerFactory(List<ICodeCleanerFactory> specializedFactories) {
		this.specializedFactories = specializedFactories;
	}

	@Override
	public Optional<IGitRefCleaner> makeCleaner(Object somethingInteresting) {
		return specializedFactories.stream()
				.map(f -> f.makeCleaner(somethingInteresting))
				.flatMap(Optional::stream)
				.findFirst();
	}

}
