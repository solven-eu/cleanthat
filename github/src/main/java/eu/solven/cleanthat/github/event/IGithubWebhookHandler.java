package eu.solven.cleanthat.github.event;

import java.util.Map;

import org.kohsuke.github.GitHub;

import eu.solven.cleanthat.github.IStringFormatter;

/**
 * Knows how to process a Github webhook
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubWebhookHandler {

	Map<String, ?> processWebhookBody(Map<String, ?> input, IStringFormatter formatter);

	GitHub getGithub();

	GitHub makeInstallationGithub(long installationId);

}
