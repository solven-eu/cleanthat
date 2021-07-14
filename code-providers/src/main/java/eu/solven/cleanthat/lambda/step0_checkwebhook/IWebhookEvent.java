package eu.solven.cleanthat.lambda.step0_checkwebhook;

import java.util.Map;

/**
 * An event, would it come from an external source (e.g. Github webhook), or internally (i.e. from CleanThat itself, for
 * event processing)
 * 
 * @author Benoit Lacelle
 *
 */
public interface IWebhookEvent {

	/**
	 * At github, headers holds details like the type of webhook (e.g. pull_requests, push, ...)
	 * 
	 * @return
	 */
	Map<String, ?> getHeaders();

	Map<String, ?> getBody();

}
