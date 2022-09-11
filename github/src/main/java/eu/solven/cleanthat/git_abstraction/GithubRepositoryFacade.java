package eu.solven.cleanthat.git_abstraction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.github.CleanthatRefFilterProperties;

/**
 * Enable a Facade over RewiewRequestProvider
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubRepositoryFacade {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRepositoryFacade.class);

	final GHRepository repository;

	public GithubRepositoryFacade(GHRepository repository) {
		this.repository = repository;
	}

	public Stream<GHPullRequest> findAnyPrHeadMatchingRef(String ref) throws IOException {
		return repository.getPullRequests(GHIssueState.OPEN).stream().filter(pr -> {
			return ref.equals(GithubFacade.toFullGitRef(pr.getHead()));
		});
	}

	public Optional<GHPullRequest> findFirstPrBaseMatchingRef(String ref) throws IOException {
		return repository.getPullRequests(GHIssueState.OPEN).stream().filter(pr -> {
			return ref.equals(GithubFacade.toFullGitRef(pr.getBase()));
		}).findAny();
	}

	public Optional<?> openPrIfNoneExists(GitRepoBranchSha1 base, GitRepoBranchSha1 head, String title, String body)
			throws IOException {
		String repoName = getRepoFullName();
		if (!base.getRepoFullName().equals(repoName)) {
			throw new IllegalArgumentException(
					"The base (" + base + ") is not a ref of current repo (" + repoName + ")");
		} else if (!head.getRepoFullName().equals(repoName)) {
			// TODO We would need the head username to build a headRef like 'username:branch'
			// This way rely on the match Github allows a single fork per username/organisation
			throw new UnsupportedOperationException(
					"The head (" + head + ") has a different repository than current repo (" + repoName + ")");
		}

		String baseFullRef = base.getRef();
		if (!baseFullRef.startsWith(CleanthatRefFilterProperties.BRANCHES_PREFIX)) {
			throw new IllegalArgumentException("The base has to be a branch. ref=" + baseFullRef);
		}
		String headRef = head.getRef();

		Optional<GHPullRequest> existingPr = repository.getPullRequests(GHIssueState.ALL).stream().filter(pr -> {
			return baseFullRef.equals(GithubFacade.toFullGitRef(pr.getBase()))
					&& headRef.equals(GithubFacade.toFullGitRef(pr.getHead()));
		}).findAny();

		if (existingPr.isEmpty()) {
			// TODO We should allow ourselves to write into a previous RR open by CleanThat (else we would open an
			// infinite amount of cleaning PR, all with the same content)

			String baseBranchName = baseFullRef.substring(CleanthatRefFilterProperties.BRANCHES_PREFIX.length());

			// We create only Draft PR for now
			boolean isDraft = true;
			// Maintainers are of-course allowed to modify CleanThat PR
			GHPullRequest pr = repository.createPullRequest(title, headRef, baseBranchName, body, true, isDraft);

			return Optional.of(pr.getNumber());
		} else {
			LOGGER.info("Existing PR: {}", existingPr.get());
			return Optional.empty();
		}
	}

	public String getRepoFullName() {
		return repository.getFullName();
	}

	public void removeRef(GitRepoBranchSha1 ref) throws IOException {
		String repoName = getRepoFullName();
		if (!repoName.equals(ref.getRepoFullName())) {
			throw new IllegalArgumentException("Inconsistent repo: " + repoName + "and " + ref.getRepoFullName());
		}

		GHRef remoteRef = getRef(ref.getRef());

		URL remoteRefUrl = remoteRef.getUrl();
		String currentSha = remoteRef.getObject().getSha();
		String initialSha = ref.getSha();
		if (currentSha.equals(initialSha)) {
			LOGGER.info("About to delete {}", remoteRefUrl);
			remoteRef.delete();
			LOGGER.info("Deleted {}", remoteRefUrl);
		} else {
			LOGGER.info("We skip removal of {} as its current sha ({}) differs from the sha at creation ({})",
					remoteRefUrl,
					currentSha,
					initialSha);
		}
	}

	/**
	 * 
	 * @param refName
	 *            a refName, starting with 'refs/'
	 * @return
	 * @throws IOException
	 */
	public GHRef getRef(String refName) throws IOException {
		if (!refName.startsWith(CleanthatRefFilterProperties.REFS_PREFIX)) {
			throw new IllegalArgumentException("Invalid ref: " + refName);
		}

		// repository.getRef expects a ref name without the leading 'refs/'
		String githubRefName = refName.substring(CleanthatRefFilterProperties.REFS_PREFIX.length());

		return repository.getRef(githubRefName);
	}

	public GHRepository getRepository() {
		return repository;
	}

	public GHCommit getCommit(String sha1) {
		try {
			return repository.getCommit(sha1);
		} catch (IOException e) {
			throw new UncheckedIOException(
					"Issue fetching commit for sha1=" + sha1 + " (repo=" + repository.getHtmlUrl() + ")",
					e);
		}
	}

}
