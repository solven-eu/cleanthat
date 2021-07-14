package eu.solven.cleanthat.github.event;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * Factory for {@link IGithubRefCleaner}
 * 
 * @author Benoit Lacelle
 *
 */
public class CodeCleanerFactory implements ICodeCleanerFactory {

	final ObjectMapper objectMapper;
	final ICodeProviderFormatter formatterProvider;

	public CodeCleanerFactory(ObjectMapper objectMapper, ICodeProviderFormatter formatterProvider) {
		this.objectMapper = objectMapper;
		this.formatterProvider = formatterProvider;
	}

	@Override
	public IGithubRefCleaner makeCleaner(Object somethingInteresting) {
		if (somethingInteresting instanceof GithubAndToken) {
			return new GithubRefCleaner(objectMapper, formatterProvider, (GithubAndToken) somethingInteresting);
		}
		throw new IllegalArgumentException("Invalid argument:" + somethingInteresting);
	}

}
