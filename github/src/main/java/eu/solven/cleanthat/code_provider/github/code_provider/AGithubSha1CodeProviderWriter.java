package eu.solven.cleanthat.code_provider.github.code_provider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.refs.GithubRefWriterLogic;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubSha1CodeProviderWriter extends AGithubSha1CodeProvider
		implements ICodeProviderWriter, IGithubSha1CodeProvider {
	// private static final Logger LOGGER = LoggerFactory.getLogger(AGithubSha1CodeProviderWriter.class);

	public AGithubSha1CodeProviderWriter(String token, GHRepository repo) {
		super(token, repo);
	}

	protected abstract GHRef getAsGHRef();

	@Override
	public void persistChanges(Map<String, String> pathToMutatedContent,
			List<String> prComments,
			Collection<String> prLabels) {
		new GithubRefWriterLogic(repo, getAsGHRef()).persistChanges(pathToMutatedContent, prComments, prLabels);
	}

	@Override
	public void cleanTmpFiles() {
		helper.cleanTmpFiles();
	}

}
