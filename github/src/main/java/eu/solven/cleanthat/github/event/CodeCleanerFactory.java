package eu.solven.cleanthat.github.event;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.github.refs.IGithubRefCleaner;

/**
 * Factory for {@link IGithubRefCleaner}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeCleanerFactory implements ICodeCleanerFactory {

	final List<ObjectMapper> objectMappers;
	final ICodeProviderFormatter formatterProvider;

	public CodeCleanerFactory(List<ObjectMapper> objectMappers, ICodeProviderFormatter formatterProvider) {
		this.objectMappers = objectMappers;
		this.formatterProvider = formatterProvider;
	}

	@Override
	public IGithubRefCleaner makeCleaner(Object somethingInteresting) {
		if (somethingInteresting instanceof GithubAndToken) {
			return new GithubRefCleaner(objectMappers, formatterProvider, (GithubAndToken) somethingInteresting);
		}
		throw new IllegalArgumentException("Invalid argument:" + somethingInteresting);
	}

}
