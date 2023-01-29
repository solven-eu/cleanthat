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
package org.kohsuke.github;

import org.kohsuke.github.internal.Previews;

// https://github.com/hub4j/github-api/issues/1082
public class GitHubClientUtil {
	public static PagedSearchIterable<GHRepository> listRepositories(GitHub gitHub) {
		GitHubRequest request = ((Requester) ((Requester) gitHub.createRequest().withPreview(Previews.MACHINE_MAN))
				.withUrlPath("/installation/repositories", new String[0])).build();
		return new PagedSearchIterable<>(gitHub, request, GHAppInstallationRepositoryResult.class);
	}

	private static class GHAppInstallationRepositoryResult extends SearchResult<GHRepository> {
		private GHRepository[] repositories;

		private GHAppInstallationRepositoryResult() {
		}

		GHRepository[] getItems(GitHub root) {
			return this.repositories;
		}
	}
}