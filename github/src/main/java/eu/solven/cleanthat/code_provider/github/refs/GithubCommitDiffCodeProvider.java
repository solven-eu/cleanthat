package eu.solven.cleanthat.code_provider.github.refs;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubCommitDiffCodeProvider extends AGithubDiffCodeProvider implements IListOnlyModifiedFiles {
	// private static final Logger LOGGER = LoggerFactory.getLogger(GithubCommitDiffCodeProvider.class);

	final GHCommit base;
	final GHCommit head;

	public GithubCommitDiffCodeProvider(String token, GHRepository baseRepository, GHCommit base, GHCommit head) {
		super(token, baseRepository);

		this.base = base;
		this.head = head;
	}

	@Override
	protected String getBaseId() {
		return base.getSHA1();
	}

	@Override
	protected String getHeadId() {
		return head.getSHA1();
	}

}
