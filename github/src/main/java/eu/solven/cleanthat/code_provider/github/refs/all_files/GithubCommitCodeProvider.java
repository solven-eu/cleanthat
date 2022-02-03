package eu.solven.cleanthat.code_provider.github.refs.all_files;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubCommitCodeProvider extends AGithubSha1CodeProvider {
	final GHCommit commit;

	public GithubCommitCodeProvider(String token, GHRepository repo, GHCommit commit) {
		super(token, repo);
		this.commit = commit;
	}

	@Override
	public String getHtmlUrl() {
		return "TODO Branch URL";
	}

	@Override
	public String getTitle() {
		return getSha1();
	}

	@Override
	public String getSha1() {
		return commit.getSHA1();
	}

	@Override
	public String getRef() {
		return getSha1();
	}

}
