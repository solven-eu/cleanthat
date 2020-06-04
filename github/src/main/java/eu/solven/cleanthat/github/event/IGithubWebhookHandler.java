package eu.solven.cleanthat.github.event;

import java.util.Map;

import org.kohsuke.github.GitHub;

/**
 * Knows how to process a Github webhook
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubWebhookHandler {

	Map<String, ?> processWebhookBody(Map<String, ?> input);

	GitHub getGithub();

	GitHub makeInstallationGithub(long installationId);

}
