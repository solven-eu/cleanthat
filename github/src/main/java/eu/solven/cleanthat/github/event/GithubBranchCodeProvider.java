package eu.solven.cleanthat.github.event;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.jgit.JGitCodeProvider;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubBranchCodeProvider extends AGithubSha1CodeProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubBranchCodeProvider.class);

	final GHBranch branch;

	final AtomicReference<JGitCodeProvider> localClone = new AtomicReference<>();

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
	protected String getSha1() {
		return branch.getSHA1();
	}

	@Override
	protected String getRef() {
		return branch.getName();
	}

}
