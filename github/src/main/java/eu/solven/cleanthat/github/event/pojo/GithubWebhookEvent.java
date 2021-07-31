package eu.solven.cleanthat.github.event.pojo;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Strings;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * a POJO holding details about Github webhooks
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubWebhookEvent implements I3rdPartyWebhookEvent {
	final String xGithubEvent;
	final String xGithubDelivery;
	final String xHubSignature256;

	final Map<String, ?> body;

	public GithubWebhookEvent(String xGithubEvent,
			String xGithubDelivery,
			String xGithubSignature256,
			Map<String, ?> body) {
		this.xGithubEvent = xGithubEvent;
		this.xGithubDelivery = xGithubDelivery;
		this.xHubSignature256 = xGithubSignature256;
		this.body = body;
	}

	public GithubWebhookEvent(Map<String, ?> body) {
		this.xGithubEvent = "";
		this.xGithubDelivery = "";
		this.xHubSignature256 = "";
		this.body = body;
	}

	public String getxGithubEvent() {
		return xGithubEvent;
	}

	public String getxGithubDelivery() {
		return xGithubDelivery;
	}

	public String getxHubSignature256() {
		return xHubSignature256;
	}

	@Override
	public Map<String, ?> getBody() {
		return body;
	}

	@Override
	public Map<String, ?> getHeaders() {
		Map<String, Object> headers = new LinkedHashMap<>();

		if (!Strings.isNullOrEmpty(xGithubEvent)) {
			headers.put("X-GitHub-Event", xGithubEvent);
		}
		if (!Strings.isNullOrEmpty(xGithubDelivery)) {
			headers.put("X-GitHub-Delivery", xGithubDelivery);
		}
		if (!Strings.isNullOrEmpty(xHubSignature256)) {
			headers.put("X-Hub-Signature-256", xHubSignature256);
		}

		return headers;
	}

	public static GithubWebhookEvent fromCleanThatEvent(IWebhookEvent githubAcceptedEvent) {
		return new GithubWebhookEvent(
				PepperMapHelper.getRequiredMap(githubAcceptedEvent.getBody(), "body", "github", "body"));
	}
}
