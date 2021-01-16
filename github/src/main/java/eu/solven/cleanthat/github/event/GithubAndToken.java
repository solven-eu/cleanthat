package eu.solven.cleanthat.github.event;

import org.kohsuke.github.GitHub;

/**
 * Wraps a Github instance and a token
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubAndToken {
	private final GitHub github;
	private final String token;

	public GithubAndToken(GitHub github, String token) {
		this.github = github;
		this.token = token;
	}

	public GitHub getGithub() {
		return github;
	}

	public String getToken() {
		return token;
	}
}
