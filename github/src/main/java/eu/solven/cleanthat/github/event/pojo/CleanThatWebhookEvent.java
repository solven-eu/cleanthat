package eu.solven.cleanthat.github.event.pojo;

import java.util.Map;

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * A generate {@link IWebhookEvent} used internally in CleanThat pipeline
 * 
 * @author Benoit Lacelle
 *
 */
public class CleanThatWebhookEvent implements IWebhookEvent {
	final Map<String, ?> headers;
	final Map<String, ?> body;

	public CleanThatWebhookEvent(Map<String, ?> headers, Map<String, ?> body) {
		this.headers = headers;
		this.body = body;
	}

	@Override
	public Map<String, ?> getHeaders() {
		return headers;
	}

	@Override
	public Map<String, ?> getBody() {
		return body;
	}

}
