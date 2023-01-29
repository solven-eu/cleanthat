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

/**
 * Given PR may be cross repositories, we need to qualify the head by the actual repository (which may differ from the
 * base branch repo).
 * 
 * @author Benoit Lacelle
 *
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#webhook-payload-example-32
public class GitPrHeadRef {
	final String repoName;
	final Object id;

	final String baseRef;
	final String headRef;

	public GitPrHeadRef(String repoName, Object id, String baseRef, String headRef) {
		this.repoName = repoName;
		this.id = id;
		this.baseRef = baseRef;
		this.headRef = headRef;
	}

	public String getRepoName() {
		return repoName;
	}

	public Object getId() {
		return id;
	}

	public String getBaseRef() {
		return baseRef;
	}

	public String getHeadRef() {
		return headRef;
	}
}
