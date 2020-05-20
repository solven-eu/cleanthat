package eu.solven.cleanthat.lambda;

import java.util.Map;
import java.util.Optional;

import org.kohsuke.github.GitHub;
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
	public Map<String, ?> processWebhookBody(Map<String, ?> input) {
		// https://developer.github.com/webhooks/event-payloads/

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

		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#http-based-git-access-by-an-installation
		// git clone https://x-access-token:<token>@github.com/owner/repo.git

		return Map.of();
	}

}
