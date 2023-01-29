/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.code_provider.github.event;

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
