package eu.solven.cleanthat.git_abstraction;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.github.CleanthatRefFilterProperties;

/**
 * Enable a Facade over RewiewRequestProvider
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubFacade {
	// private static final Logger LOGGER = LoggerFactory.getLogger(GithubFacade.class);

	final GitHub github;
	final String repoName;

	final GithubRepositoryFacade repoFacade;

	public GithubFacade(GitHub github, String repoName) throws IOException {
		this.github = github;
		this.repoName = repoName;

		this.repoFacade = new GithubRepositoryFacade(github.getRepository(repoName));
	}

	public static String toFullGitRef(GHCommitPointer ghCommitPointer) {
		String githubRef = ghCommitPointer.getRef();
		return toFullGitRef(githubRef);
	}

	public static String toFullGitRef(String githubRef) {
		return CleanthatRefFilterProperties.BRANCHES_PREFIX + githubRef;
	}

	@Deprecated
	public Stream<GHPullRequest> findAnyPrHeadMatchingRef(String ref) throws IOException {
		return repoFacade.findAnyPrHeadMatchingRef(ref);
	}

	@Deprecated
	public Optional<GHPullRequest> findFirstPrBaseMatchingRef(String ref) throws IOException {
		return repoFacade.findFirstPrBaseMatchingRef(ref);
	}

	@Deprecated
	public Optional<?> openPrIfNoneExists(GitRepoBranchSha1 base, GitRepoBranchSha1 head, String title, String body)
			throws IOException {
		return repoFacade.openPrIfNoneExists(base, head, title, body);
	}

	@Deprecated
	public void removeRef(GitRepoBranchSha1 ref) throws IOException {
		repoFacade.removeRef(ref);
	}

	@Deprecated
	public GHRef getRef(String refName) throws IOException {
		return repoFacade.getRef(refName);
	}

	public GHRepository getRepository() {
		return repoFacade.getRepository().getDecorated();
	}

}
