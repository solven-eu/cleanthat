package eu.solven.cleanthat.github.event;

import java.util.Map;

import org.kohsuke.github.GHPermissionType;
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

	// https://github.com/organizations/solven-eu/settings/apps/cleanthat/permissions
	// On Permissions change, invite people to go into:
	// https://github.com/organizations/solven-eu/settings/installations/9086720
	private final Map<String, GHPermissionType> permissions;

	public GithubAndToken(GitHub github, String token, Map<String, GHPermissionType> permissions) {
		this.github = github;
		this.token = token;
		this.permissions = permissions;
	}

	public GitHub getGithub() {
		return github;
	}

	public String getToken() {
		return token;
	}

	public Map<String, GHPermissionType> getPermissions() {
		return permissions;
	}
}
