package eu.solven.cleanthat.code_provider.github.event;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GitHub;

/**
 * Knows how to process a Github webhook
 *
 * @author Benoit Lacelle
 */
public interface IGithubWebhookHandler extends IGitWebhookHandler {
	/**
	 * Typically useful to list installations
	 * 
	 * @return a {@link GitHub} instance authenticated as the Github Application.
	 */
	GHApp getGithubAsApp();

	/**
	 * 
	 * @param installationId
	 * @return a {@link GitHub} instance authenticated as given installation, having access to permitted repositories
	 */
	GithubAndToken makeInstallationGithub(long installationId);

}
