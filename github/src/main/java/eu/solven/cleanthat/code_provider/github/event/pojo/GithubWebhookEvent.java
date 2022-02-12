package eu.solven.cleanthat.code_provider.github.event.pojo;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Strings;

import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.pepper.collection.PepperMapHelper;

/**
 * a POJO holding details about Github webhooks
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubWebhookEvent implements I3rdPartyWebhookEvent {
	public static final String X_GIT_HUB_DELIVERY = "X-GitHub-Delivery";

	private static final String KEY_BODY = "body";
	private static final String KEY_GITHUB = "github";
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
			headers.put(X_GIT_HUB_DELIVERY, xGithubDelivery);
		}
		if (!Strings.isNullOrEmpty(xHubSignature256)) {
			headers.put("X-Hub-Signature-256", xHubSignature256);
		}

		return headers;
	}

	public static GithubWebhookEvent fromCleanThatEvent(IWebhookEvent githubAcceptedEvent) {
		Map<String, ?> body = githubAcceptedEvent.getBody();

		while (!body.containsKey(KEY_GITHUB) && body.containsKey(KEY_BODY)) {
			body = PepperMapHelper.getRequiredMap(githubAcceptedEvent.getBody(), KEY_BODY);
		}

		if (!body.containsKey(KEY_GITHUB)) {
			throw new IllegalArgumentException("This does not hold a github event");
		}

		Map<String, ?> headers = PepperMapHelper.getRequiredMap(body, KEY_GITHUB, "headers");
		String xGithubEvent = PepperMapHelper.getOptionalString(headers, "X-GitHub-Event").orElse("");
		String xGithubDelivery = PepperMapHelper.getOptionalString(headers, X_GIT_HUB_DELIVERY).orElse("");
		String xGithubSignature256 = PepperMapHelper.getOptionalString(headers, "X-GitHub-Signature-256").orElse("");

		return new GithubWebhookEvent(xGithubEvent,
				xGithubDelivery,
				xGithubSignature256,
				PepperMapHelper.getRequiredMap(body, KEY_GITHUB, KEY_BODY));
	}
}
