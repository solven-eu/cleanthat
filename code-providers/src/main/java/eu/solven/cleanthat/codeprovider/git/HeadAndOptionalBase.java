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

import java.util.Optional;

/**
 * This helps materializing a head to be cleaned, and an optional base. the base is missing when the head is directly
 * cleanable. The base might be a PR base, or a temporary branch to clean a branch without any PR.
 * 
 * @author Benoit Lacelle
 *
 */
public class HeadAndOptionalBase {
	// If present, it means the webhook is relevant and here we provide the essence of the commit (e.g. is this a push
	// to a ref, or a PR being open, or...)
	final GitRepoBranchSha1 headToClean;

	// If we have a base: it needs we should clean only the diff between the base and the branch
	final Optional<GitRepoBranchSha1> oBaseforHead;

	public HeadAndOptionalBase(GitRepoBranchSha1 headToClean, Optional<GitRepoBranchSha1> optBaseToConsider) {
		this.headToClean = headToClean;
		this.oBaseforHead = optBaseToConsider;
	}

	public GitRepoBranchSha1 getHeadToClean() {
		return headToClean;
	}

	public Optional<GitRepoBranchSha1> optBaseForHead() {
		return oBaseforHead;
	}
}
