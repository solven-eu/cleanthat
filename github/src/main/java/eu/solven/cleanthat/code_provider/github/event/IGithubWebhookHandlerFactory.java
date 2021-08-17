package eu.solven.cleanthat.code_provider.github.event;

import java.io.IOException;

import org.kohsuke.github.GitHub;

/**
 * Prepare an authenticated {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubWebhookHandlerFactory {

	IGithubWebhookHandler makeWithFreshJwt() throws IOException;

}
