package eu.solven.cleanthat.code_provider.github.code_provider;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriterLogic;

/**
 * Specific to Github sha1 {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubSha1CodeProviderWriter extends IGithubSha1CodeProvider, ICodeProviderWriterLogic {

	@Override
	String getRef();

}
