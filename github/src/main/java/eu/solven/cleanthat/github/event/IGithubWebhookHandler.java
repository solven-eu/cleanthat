package eu.solven.cleanthat.github.event;

import java.util.Map;

import org.kohsuke.github.GitHub;

/**
 * Knows how to process a Github webhook
 *
 * @author Benoit Lacelle
 */
public interface IGithubWebhookHandler {

	Map<String, ?> processWebhookBody(Map<String, ?> input, IGithubPullRequestCleaner prCleaner);

	/**
	 * Typically useful to list installations
	 * 
	 * @return a {@link GitHub} instance authenticated as the Github Application.
	 */
	GitHub getGithubAsApp();

	/**
	 * 
	 * @param installationId
	 * @return a {@link GitHub} instance authenticated as given installation, having access to permitted repositories
	 */
	GithubAndToken makeInstallationGithub(long installationId);
}
