package eu.solven.cleanthat.code_provider.github.code_provider;

import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriterLogic;

/**
 * Specific to Github sha1 {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubSha1CodeProvider extends ICodeProviderWriterLogic {

	String getSha1();

	String getRef();

	GHRepository getRepo();

	String getToken();

}
