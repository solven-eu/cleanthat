package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import cormoran.pepper.jvm.GCInspector;

/**
 * Default implementation for IGithubWebhookHandler
 * 
 * @author Benoit Lacelle
 *
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads
public class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	private static final String KEY_SKIPPED = "skipped";

	final GitHub github;
	final ObjectMapper objectMapper;

	public GithubWebhookHandler(GitHub github, ObjectMapper objectMapper) {
		this.github = github;
		this.objectMapper = objectMapper;
	}

	@Override
	public GitHub getGithubAsApp() {
		return github;
	}

	@Override
	public GithubAndToken makeInstallationGithub(long installationId) {
		try {
			GHAppInstallation installationById = github.getApp().getInstallationById(installationId);

			LOGGER.info("Permissions: {}", installationById.getPermissions());
			LOGGER.info("RepositorySelection: {}", installationById.getRepositorySelection());

			// https://github.com/hub4j/github-api/issues/570
			GHAppCreateTokenBuilder installationGithub = installationById.createToken(Map.of(
					// Required to open new pull-requests
					"pull_requests",
					GHPermissionType.WRITE,
					// Required to access a repository without having to list all available repositories
					"metadata",
					GHPermissionType.READ,
					// Required to read files, and commit new versions
					"contents",
					GHPermissionType.WRITE));

			// https://github.com/hub4j/github-api/issues/570
			String token = installationGithub.create().getToken();

			return new GithubAndToken(makeInstallationGithub(token), token);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected GitHub makeInstallationGithub(String token) throws IOException {
		return new GitHubBuilder().withAppInstallationToken(token).build();
	}

	@SuppressWarnings({ "PMD.ExcessiveMethodLength", "checkstyle:MethodLength", "PMD.NPathComplexity" })
	@Override
	public Map<String, ?> processWebhookBody(Map<String, ?> input, IGithubPullRequestCleaner prCleaner) {
		// https://developer.github.com/webhooks/event-payloads/

		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		Optional<Object> organizationUrl = Optional.ofNullable(PepperMapHelper.getAs(input, "organization", "url"));
		LOGGER.info("Received a webhook for installationId={} (organization={})",
				installationId,
				organizationUrl.orElse("-"));

		// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#push
		// Push on PR: there is no action. There may be multiple commits being pushed

		Optional<String> optAction = PepperMapHelper.getOptionalString(input, "action");

		if (optAction.isPresent()) {
			String action = optAction.get();

			if (Set.of("created", "deleted", "uspend", "unsuspend", "new_permissions_accepted").contains(action)) {
				// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#installation_repositories
				LOGGER.info("We are not interested in action={} (installation)", action);

				return Map.of(KEY_SKIPPED, "Github action: " + action);
			} else if (Set.of("added", "removed").contains(action)) {
				// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#installation_repositories
				LOGGER.info("We are not interested in action={} (installation_repositories)", action);

				return Map.of(KEY_SKIPPED, "Github action: " + action);
			}
		}

		// We log the payload temporarily, in order to have easy access to metadata
		if (!GCInspector.inUnitTest()) {
			try {
				LOGGER.info("TMP payload: {}", objectMapper.writeValueAsString(input));
			} catch (JsonProcessingException e) {
				LOGGER.warn("Issue while printing the json of the webhook", e);
			}
		}

		GithubAndToken githubAuthAsInst = makeInstallationGithub(installationId);
		GHRepository repo;
		try {
			repo = githubAuthAsInst.getGithub()
					.getRepositoryById(
							Long.toString(PepperMapHelper.getRequiredNumber(input, "repository", "id").longValue()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Optional<String> ref = PepperMapHelper.getOptionalString(input, "ref");

		String defaultBranch = GitHelper.getDefaultBranch(Optional.ofNullable(repo.getDefaultBranch()));

		final boolean isBranchWithoutPR;
		final boolean isMainBranchCommit;
		final boolean isBranchDeleted;

		Optional<GHPullRequest> optPr;
		if (ref.isPresent()) {
			if (Boolean.TRUE.equals(input.get("deleted"))) {
				LOGGER.info("This is the deletion of ref={}", ref.get());
				isBranchDeleted = true;
			} else {
				// https://developer.github.com/webhooks/event-payloads/#push
				LOGGER.info("We are notified of a new commit on ref={}", ref.get());
				isBranchDeleted = false;
			}

			if (defaultBranch.equals("refs/heads/" + ref.get())) {
				isMainBranchCommit = true;
			} else {
				isMainBranchCommit = false;
			}

			// We search for a matching PR, as in such a case, we would wait for a relevant PR event (is there a PR
			// event when its branch changes HEAD?)
			// NO, it is rather interesting for the case there is no PR. Then, we could try to process changed
			// files, in order to manage PR-less branches.
			// Still, this seems a very complex features, as it is difficult to know from which branch/the full
			// commit-list we have to process
			try {
				optPr = repo.getPullRequests(GHIssueState.OPEN).stream().filter(pr -> {
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
			isMainBranchCommit = false;
			isBranchWithoutPR = false;
			isBranchDeleted = false;

			Map<String, ?> pullRequest = PepperMapHelper.getAs(input, "pull_request");

			String actionOrMissingMarker = optAction.orElse("<missing>");
			if (pullRequest == null) {
				// TODO When does this happen?
				LOGGER.info("We are not interested in action={} as no pull_request", actionOrMissingMarker);
				optPr = Optional.empty();
			} else {
				// https://developer.github.com/webhooks/event-payloads/#pull_request
				if (!"opened".equals(actionOrMissingMarker)) {
					LOGGER.info("We are not interested in action={}", actionOrMissingMarker);
					return Map.of("action", "discarded");
				} else {
					String url = PepperMapHelper.getRequiredString(input, "pull_request", "url");
					// We need the PR number (and not its id)
					int prId = PepperMapHelper.getRequiredNumber(input, "pull_request", "number").intValue();
					LOGGER.info("We are notified of a new PR: {}", url);

					try {
						optPr = Optional.of(repo.getPullRequest(prId));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		}
		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#http-based-git-access-by-an-installation
		// git clone https://x-access-token:<token>@github.com/owner/repo.git

		if (isBranchDeleted) {
			return Map.of(KEY_SKIPPED, "webhook triggered on a branch deletion");
		} else {
			LOGGER.info("Notified commit on main branch: {}", isMainBranchCommit);
			CommitContext commitContext = new CommitContext(isMainBranchCommit, isBranchWithoutPR);

			if (optPr.isPresent()) {
				GHPullRequest pr = optPr.get();
				if (pr.isLocked()) {
					LOGGER.info("PR is locked: {}", pr.getHtmlUrl());
					return Map.of(KEY_SKIPPED, "PullRequest is locked");
				} else {
					try {
						GHUser user = pr.getUser();
						// TODO Do not process PR opened by CleanThat
						LOGGER.info("user_id={} ({})", user.getId(), user.getHtmlUrl());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}

					return prCleaner.formatPR(githubAuthAsInst.getToken(), commitContext, optPr::get);
				}
			} else {
				return Map.of(KEY_SKIPPED, "webhook is not attached to a PullRequest");
			}
		}
	}

}
