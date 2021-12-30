package eu.solven.cleanthat.code_provider.github.event;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.language.ILanguageLintFixerFactory;

/**
 * {@link ICodeCleanerFactory} specialized for GitHub
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubCodeCleanerFactory implements ICodeCleanerFactory {
	final List<ObjectMapper> objectMappers;
	final List<ILanguageLintFixerFactory> factories;
	final ICodeProviderFormatter formatterProvider;

	public GithubCodeCleanerFactory(List<ObjectMapper> objectMappers,
			List<ILanguageLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider) {
		this.objectMappers = objectMappers;
		this.factories = factories;
		this.formatterProvider = formatterProvider;
	}

	@Override
	public Optional<IGitRefCleaner> makeCleaner(Object somethingInteresting) {
		if (somethingInteresting instanceof GithubAndToken) {
			GithubRefCleaner refCleaner = new GithubRefCleaner(objectMappers,
					factories,
					formatterProvider,
					(GithubAndToken) somethingInteresting);
			return Optional.of(refCleaner);
		}
		return Optional.empty();
	}
}
