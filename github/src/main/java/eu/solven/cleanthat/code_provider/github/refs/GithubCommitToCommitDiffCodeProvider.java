package eu.solven.cleanthat.code_provider.github.refs;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;

/**
 * An {@link ICodeProvider} from a ref to a commit
 *
 * @author Benoit Lacelle
 */
public class GithubCommitToCommitDiffCodeProvider extends AGithubDiffCodeProvider implements IListOnlyModifiedFiles {
	final GHCommit base;
	final GHCommit head;

	public GithubCommitToCommitDiffCodeProvider(String token,
			GHRepository baseRepository,
			GHCommit base,
			GHCommit head) {
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
