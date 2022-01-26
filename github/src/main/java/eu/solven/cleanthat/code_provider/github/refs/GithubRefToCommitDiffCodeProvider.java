package eu.solven.cleanthat.code_provider.github.refs;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;

/**
 * An {@link ICodeProvider} from a ref to a commit
 *
 * @author Benoit Lacelle
 */
public class GithubRefToCommitDiffCodeProvider extends AGithubDiffCodeProvider implements IListOnlyModifiedFiles {
	final GHRef base;
	final GHCommit head;

	public GithubRefToCommitDiffCodeProvider(String token, GHRepository baseRepository, GHRef base, GHCommit head) {
		super(token, baseRepository);

		this.base = base;
		this.head = head;
	}

	@Override
	protected String getBaseId() {
		return base.getRef();
	}

	@Override
	protected String getHeadId() {
		return head.getSHA1();
	}

}
