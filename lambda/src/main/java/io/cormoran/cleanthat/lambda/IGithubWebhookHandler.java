package io.cormoran.cleanthat.lambda;

import java.util.Map;

public interface IGithubWebhookHandler {

	Map<String, ?> processWebhookBody(Map<String, ?> input);

}
