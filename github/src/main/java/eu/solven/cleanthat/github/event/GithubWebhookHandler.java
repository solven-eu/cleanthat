package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cormoran.pepper.collection.PepperMapHelper;

/**
 * Default implementation for IGithubWebhookHandler
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	final GitHub github;

	public GithubWebhookHandler(GitHub github) {
		this.github = github;
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
			githubAuthAsInst =
					new GitHubBuilder().withAppInstallationToken(installationGithub.create().getToken()).build();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return githubAuthAsInst;
	}

	@Override
	public Map<String, ?> processWebhookBody(Map<String, ?> input) {
		// https://developer.github.com/webhooks/event-payloads/

		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		LOGGER.info("Received a webhook for installationId={}", installationId);

		Optional<String> ref = PepperMapHelper.getOptionalString(input, "ref");

		if (ref.isPresent()) {
			// https://developer.github.com/webhooks/event-payloads/#push
			// if (!"created".equals(action)) {
			// LOGGER.info("We are not interested in action={}", action);
			// return Map.of("action", "discarded");
			// } else {
			LOGGER.info("We are notified of a new commit: {}", ref.get());
			// }
		} else {
			String action = PepperMapHelper.getRequiredString(input, "action");
			Map<String, ?> pullRequest = PepperMapHelper.getAs(input, "pull_request");

			if (pullRequest == null) {
				LOGGER.info("We are not interested in action={} as no pull_request", action);
			} else {
				// https://developer.github.com/webhooks/event-payloads/#pull_request
				if (!"opened".equals(action)) {
					LOGGER.info("We are not interested in action={}", action);
					return Map.of("action", "discarded");
				} else {
					String url = PepperMapHelper.getRequiredString(input, "url");
					LOGGER.info("We are notified of a new PR: {}", url);
				}
			}
		}

		GitHub githubAuthAsInst = makeInstallationGithub(installationId);

		try {
			GHRepository repoId =
					githubAuthAsInst.getRepositoryById(PepperMapHelper.getRequiredString(input, "repository", "id"));

			repoId.createIssue("test").create();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#http-based-git-access-by-an-installation
		// git clone https://x-access-token:<token>@github.com/owner/repo.git

		return Map.of();
	}

}
