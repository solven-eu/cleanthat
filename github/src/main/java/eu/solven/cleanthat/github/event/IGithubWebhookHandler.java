package eu.solven.cleanthat.github.event;

import org.kohsuke.github.GitHub;

import eu.solven.cleanthat.github.event.pojo.GithubWebhookRelevancyResult;
import eu.solven.cleanthat.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * Knows how to process a Github webhook
 *
 * @author Benoit Lacelle
 */
public interface IGithubWebhookHandler {
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

	/**
	 * Offline rejection of irrelevant events (e.g. the repo is starred by anybody). This should rely exclusively on
	 * webhook content: we do not hit the CodeProvider API at all
	 * 
	 * @param input
	 * @return
	 */
	GithubWebhookRelevancyResult isWebhookEventRelevant(I3rdPartyWebhookEvent input);

	/**
	 * Online rejection of irrelevant events (e.g. there is no configuration file in the concerned branch). This can
	 * rely on the CodeProvider API to checkout single specific files (e.g. cleanthat configuration). It shall not clone
	 * the report (or checkout a large number of individual files).
	 * 
	 * @param codeCleanerFactory
	 * 
	 * @param input
	 * @return
	 */
	WebhookRelevancyResult isWebhookEventTargetRelevantBranch(ICodeCleanerFactory codeCleanerFactory,
			IWebhookEvent input);

	void doExecuteWebhookEvent(ICodeCleanerFactory codeCleanerFactory, IWebhookEvent input);
}
