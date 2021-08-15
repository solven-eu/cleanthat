package eu.solven.cleanthat.code_provider.github.code_provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class GithubSha1CodeProviderHelper extends AGithubCodeProvider implements ICodeProviderWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubSha1CodeProviderHelper.class);

	private static final int MAX_FILE_BEFORE_CLONING = 512;

	private static final boolean ZIP_ELSE_CLONE = true;

	public int getMaxFileBeforeCLoning() {
		return MAX_FILE_BEFORE_CLONING;
	}
}
