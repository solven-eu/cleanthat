package eu.solven.cleanthat.code_provider.github.refs;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubCommitToRefDiffCodeProviderWriter extends AGithubHeadRefDiffCodeProvider
		implements IListOnlyModifiedFiles, ICodeProviderWriter {
	final GHCommit base;

	public GithubCommitToRefDiffCodeProviderWriter(String token,
			GHRepository baseRepository,
			GHCommit base,
			GHRef head) {
		super(token, baseRepository, head);

		this.base = base;
	}

	@Override
	protected String getBaseId() {
		return base.getSHA1();
	}

}
