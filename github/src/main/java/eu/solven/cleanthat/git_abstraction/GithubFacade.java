package eu.solven.cleanthat.git_abstraction;

import java.io.IOException;
import java.util.Optional;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;

/**
 * Enable a Facade over RewiewRequestProvider
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubFacade {
	final GitHub github;
	final String repoName;

	public GithubFacade(GitHub github, String repoName) {
		this.github = github;
		this.repoName = repoName;
	}

	public Optional<GHPullRequest> findFirstPrHeadMatchingRef(String ref) throws IOException {
		return github.getRepository(repoName).getPullRequests(GHIssueState.OPEN).stream().filter(pr -> {
			return ref.equals("refs/heads/" + pr.getHead().getRef());
		}).findAny();
	}

	public Optional<GHPullRequest> findFirstPrBaseMatchingRef(String ref) throws IOException {
		return github.getRepository(repoName).getPullRequests(GHIssueState.OPEN).stream().filter(pr -> {
			return ref.equals("refs/heads/" + pr.getBase().getRef());
		}).findAny();
	}

}
