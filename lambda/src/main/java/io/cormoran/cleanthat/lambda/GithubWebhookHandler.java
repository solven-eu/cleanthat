package io.cormoran.cleanthat.lambda;

import java.util.Map;

import org.kohsuke.github.GitHub;

public class GithubWebhookHandler implements IGithubWebhookHandler {

	final GitHub github;

	public GithubWebhookHandler(GitHub github) {
		this.github = github;
	}

	@Override
	public Map<String, ?> processWebhookBody(Map<String, ?> input) {
		// TODO Auto-generated method stub
		return null;
	}

}
