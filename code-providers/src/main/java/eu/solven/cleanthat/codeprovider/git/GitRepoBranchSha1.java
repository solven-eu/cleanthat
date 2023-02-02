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
package eu.solven.cleanthat.codeprovider.git;

import eu.solven.cleanthat.github.IGitRefsConstants;

/**
 * Given PR may be cross repositories, we need to qualify a sha by its repo. A single sha1 may be associated to multiple
 * references.
 * 
 * @author Benoit Lacelle
 *
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#webhook-payload-example-32
public class GitRepoBranchSha1 {
	final String repoName;
	final String ref;
	final String sha;

	public GitRepoBranchSha1(String repoName, String ref, String sha) {
		if (!repoName.contains("/")) {
			throw new IllegalArgumentException("It seems we did not receive a fullName: " + repoName);
		}
		this.repoName = repoName;

		if (!ref.startsWith(IGitRefsConstants.REFS_PREFIX)) {
			throw new IllegalArgumentException("Invalid fullRef: " + ref);
		}

		this.ref = ref;
		this.sha = sha;
	}

	/**
	 * 
	 * @return the repository fullname. e.g. 'solven-eu/cleanthat'
	 */
	public String getRepoFullName() {
		return repoName;
	}

	/**
	 * 
	 * @return a full ref, always starting with 'refs/'
	 */
	public String getRef() {
		return ref;
	}

	public String getSha() {
		return sha;
	}

	@Override
	public String toString() {
		return "GitRepoBranchSha1 [repoName=" + repoName + ", ref=" + ref + ", sha=" + sha + "]";
	}

}
