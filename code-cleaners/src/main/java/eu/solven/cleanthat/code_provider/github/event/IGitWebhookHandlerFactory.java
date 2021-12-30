package eu.solven.cleanthat.code_provider.github.event;

import java.io.IOException;

/**
 * Prepare an authenticated {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGitWebhookHandlerFactory {

	IGitWebhookHandler makeWithFreshAuth() throws IOException;

}
