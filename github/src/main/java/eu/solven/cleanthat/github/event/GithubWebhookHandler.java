package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.IStringFormatter;

/**
 * Default implementation for IGithubWebhookHandler
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	final GitHub github;
	final ObjectMapper objectMapper;

	public GithubWebhookHandler(GitHub github, ObjectMapper objectMapper) {
		this.github = github;
		this.objectMapper = objectMapper;
	}

	@Override
	public GitHub getGithub() {
		return github;
	}

	@Override
	public GitHub makeInstallationGithub(long installationId) {
		GitHub githubAuthAsInst;
		try {
			GHAppInstallation installationById = github.getApp().getInstallationById(installationId);

			LOGGER.info("Permissions: {}", installationById.getPermissions());
			LOGGER.info("RepositorySelection: {}", installationById.getRepositorySelection());

			// https://github.com/hub4j/github-api/issues/570
			GHAppCreateTokenBuilder installationGithub = installationById.createToken(Map.of(
					// Required to open new pul-requests
					"pull_requests",
					GHPermissionType.WRITE,
					// Required to access a repository without having to list all available repositories
					"metadata",
					GHPermissionType.READ,
					// Required to read files, and commit new versions
					"contents",
					GHPermissionType.WRITE));

			// https://github.com/hub4j/github-api/issues/570
			githubAuthAsInst = makeInstallationGithub(installationGithub.create().getToken());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return githubAuthAsInst;
	}

	protected GitHub makeInstallationGithub(String token) throws IOException {
		return new GitHubBuilder().withAppInstallationToken(token).build();
	}

	@Override
	public Map<String, ?> processWebhookBody(Map<String, ?> input,
			IStringFormatter formatter,
			IGithubPullRequestCleaner prCleaner) {
		// https://developer.github.com/webhooks/event-payloads/

		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		Optional<Object> organizationUrl = Optional.ofNullable(PepperMapHelper.getAs(input, "organization", "url"));
		LOGGER.info("Received a webhook for installationId={} (organization={})",
				installationId,
				organizationUrl.orElse("-"));

		GitHub githubAuthAsInst = makeInstallationGithub(installationId);

		GHRepository repoId;
		try {
			repoId = githubAuthAsInst.getRepositoryById(
					Long.toString(PepperMapHelper.getRequiredNumber(input, "repository", "id").longValue()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Optional<String> ref = PepperMapHelper.getOptionalString(input, "ref");

		String defaultBranch = GitHelper.getDefaultBranch(Optional.ofNullable(repoId.getDefaultBranch()));

		boolean isBranchWithoutPR = false;
		final boolean isCommitMainBranch;

		Optional<GHPullRequest> optPr;
		if (ref.isPresent()) {
			if (Boolean.TRUE.equals(input.get("deleted"))) {
				LOGGER.info("This is the deletion of ref={}", ref.get());
			}

			// https://developer.github.com/webhooks/event-payloads/#push
			LOGGER.info("We are notified of a new commit on ref={}", ref.get());

			if (defaultBranch.equals("refs/heads/" + ref.get())) {
				isCommitMainBranch = true;
			} else {
				isCommitMainBranch = false;
			}

			// We search for a matching PR, as in such a case, we would wait for a relevant PR event (is there a PR
			// event when its branch changes HEAD?)
			// NO, it is rather interesting for the case there is no PR. Then, we could try to process changed files, in
			// order to manage PR-less branches
			try {
				optPr = repoId.getPullRequests(GHIssueState.OPEN).stream().filter(pr -> {
					return ref.get().equals("refs/heads/" + pr.getHead().getRef());
				}).findFirst();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			if (optPr.isPresent()) {
				LOGGER.info("We found an open-PR ({}) for ref={}", optPr.get().getHtmlUrl(), ref.get());
				isBranchWithoutPR = false;
			} else {
				LOGGER.info("We found no open-PR for ref={}", ref.get());
				isBranchWithoutPR = true;
			}
		} else {
			isCommitMainBranch = false;
			isBranchWithoutPR = false;

			String action = PepperMapHelper.getRequiredString(input, "action");
			Map<String, ?> pullRequest = PepperMapHelper.getAs(input, "pull_request");

			if (pullRequest == null) {
				// TODO When does this happen?
				LOGGER.info("We are not interested in action={} as no pull_request", action);
				optPr = Optional.empty();
			} else {
				// https://developer.github.com/webhooks/event-payloads/#pull_request
				if (!"opened".equals(action)) {
					LOGGER.info("We are not interested in action={}", action);
					return Map.of("action", "discarded");
				} else {
					String url = PepperMapHelper.getRequiredString(input, "pull_request", "url");
					// We need the PR number (and not its id)
					int prId = PepperMapHelper.getRequiredNumber(input, "pull_request", "number").intValue();
					LOGGER.info("We are notified of a new PR: {}", url);

					try {
						optPr = Optional.of(repoId.getPullRequest(prId));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		}
		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#http-based-git-access-by-an-installation
		// git clone https://x-access-token:<token>@github.com/owner/repo.git

		LOGGER.info("Commit on main branch: {}", isCommitMainBranch);

		if (optPr.isPresent()) {
			if (optPr.get().isLocked()) {
				LOGGER.info("PR is locked: {}", optPr.get().getHtmlUrl());
				return Map.of("skipped", "PullRequest is locked");
			} else {
				return prCleaner.formatPR(new CommitContext(isCommitMainBranch), optPr.get());
			}
		} else {
			return Map.of("skipped", "webhook is not attached to a PullRequest");
		}

	}

}
