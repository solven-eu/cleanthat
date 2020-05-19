package io.cormoran.cleanthat.lambda;

import java.util.Map;

/**
 * Knows how to process a Github webhook
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGithubWebhookHandler {

	Map<String, ?> processWebhookBody(Map<String, ?> input);

}
