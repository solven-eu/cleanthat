package eu.solven.cleanthat.code_provider.github.refs.all_files;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubSha1CodeProviderWriter;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.CleanthatRefFilterProperties;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubBranchCodeProvider extends AGithubSha1CodeProviderWriter {
	final GHBranch branch;

	public GithubBranchCodeProvider(String token, GHRepository repo, GHBranch branch) {
		super(token, repo);
		this.branch = branch;
	}

	@Override
	public String getHtmlUrl() {
		return "TODO Branch URL";
	}

	@Override
	public String getTitle() {
		return branch.getName();
	}

	@Override
	public String getSha1() {
		return branch.getSHA1();
	}

	@Override
	public String getRef() {
		return CleanthatRefFilterProperties.BRANCHES_PREFIX + branch.getName();
	}

	@Override
	protected GHRef getAsGHRef() {
		String refName = getRef();
		try {
			return new GithubRepositoryFacade(getRepo()).getRef(refName);
		} catch (IOException e) {
			throw new UncheckedIOException("Issue fetching ref=" + refName, e);
		}
	}

}
