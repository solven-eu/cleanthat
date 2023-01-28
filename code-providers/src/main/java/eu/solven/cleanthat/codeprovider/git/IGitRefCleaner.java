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

import eu.solven.cleanthat.codeprovider.decorator.IGitBranch;
import eu.solven.cleanthat.codeprovider.decorator.IGitCommit;
import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.decorator.IGitRepository;
import eu.solven.cleanthat.codeprovider.decorator.ILazyGitReference;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Holds the logic to clean a Ref (e.g. a PR, a Ref, a Branch, etc)
 *
 * @author Benoit Lacelle
 */
public interface IGitRefCleaner {

	/**
	 * 
	 * @param root
	 * @param branch
	 * @return true if we succeeded opening a PR with a relevant default configuration.
	 */
	boolean tryOpenPRWithCleanThatStandardConfiguration(Path root, IGitBranch branch);

	/**
	 * 
	 * @param offlineResult
	 * @param head
	 * @param relevantBaseBranches
	 * @return the ref to clean. Typically different to the input ref when we want to clean through a PR (e.g. not to
	 *         modify directly the dirty branch).
	 */
	Optional<HeadAndOptionalBase> prepareRefToClean(Path root,
			String eventKey,
			IExternalWebhookRelevancyResult offlineResult,
			GitRepoBranchSha1 head,
			Set<String> relevantBaseBranches);

	/**
	 * Format a whole ref, moving its head to a cleaned commit
	 * 
	 * @param repo
	 * @param branchSupplier
	 * @param headSupplier
	 * @return
	 */
	CodeFormatResult formatRef(Path root,
			IGitRepository repo,
			IGitBranch branchSupplier,
			ILazyGitReference headSupplier);

	@Deprecated
	CodeFormatResult formatRefDiff(Path root, IGitRepository repo, IGitReference base, ILazyGitReference headSupplier);

	/**
	 * Format a ref, based on its diff with a base commit
	 * 
	 * @param repo
	 * @param base
	 * @param headSupplier
	 * @return
	 */
	// @Deprecated(since = "unused?")
	CodeFormatResult formatCommitToRefDiff(Path root,
			IGitRepository repo,
			IGitCommit base,
			ILazyGitReference headSupplier);

}
