package eu.solven.cleanthat.code_provider.github.event;

import java.io.IOException;

/**
 * Prepare an authenticated {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitWebhookHandlerFactory {
	IGitWebhookHandler makeNoAuth() throws IOException;

	IGitWebhookHandler makeWithFreshAuth() throws IOException;

}
