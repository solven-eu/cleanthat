/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.decorator.ILazyGitReference;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.ICleanthatGitRefsConstants;

/**
 * Relates to actually cleaning code in Github
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubCodeCleaner {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubCodeCleaner.class);

	final Path root;
	final IGitRefCleaner cleaner;

	public GithubCodeCleaner(Path root, IGitRefCleaner cleaner) {
		this.root = root;
		this.cleaner = cleaner;
	}

	public CodeFormatResult executeClean(GHRepository repo, WebhookRelevancyResult relevancyResult, String eventKey) {
		GithubRepositoryFacade facade = new GithubRepositoryFacade(repo);
		AtomicReference<GitRepoBranchSha1> refLazyRefCreated = new AtomicReference<>();

		ILazyGitReference headSupplier = prepareHeadSupplier(relevancyResult, repo, facade, refLazyRefCreated);
		CodeFormatResult result =
				GithubEventHelper.executeCleaning(root, relevancyResult, eventKey, cleaner, facade, headSupplier);
		GithubEventHelper.optCreateBranchOpenPr(relevancyResult, facade, refLazyRefCreated, result);

		return result;
	}

	public ILazyGitReference prepareHeadSupplier(WebhookRelevancyResult relevancyResult,
			GHRepository repo,
			GithubRepositoryFacade facade,
			AtomicReference<GitRepoBranchSha1> refLazyRefCreated) {
		var refToProcess = relevancyResult.optHeadToClean().get();
		var refName = refToProcess.getRef();

		checkBranchProtection(repo, refName);

		String sha;

		boolean headIsYetToBeMaterialized;
		if (refName.startsWith(ICleanthatGitRefsConstants.PREFIX_REF_CLEANTHAT_TMPHEAD)) {
			Optional<GHRef> optAlreadyExisting = optRef(facade, refName);

			if (optAlreadyExisting.isPresent()) {
				sha = optAlreadyExisting.get().getObject().getSha();
				LOGGER.info("The ref {} already exists, with sha1={}", refName, sha);
				headIsYetToBeMaterialized = false;
			} else {
				sha = refToProcess.getSha();
				headIsYetToBeMaterialized = true;
			}
		} else {
			sha = refToProcess.getSha();
			headIsYetToBeMaterialized = false;
		}

		Supplier<IGitReference> headSupplier = () -> {
			Optional<GHRef> optAlreadyExisting = optRef(facade, refName);
			if (headIsYetToBeMaterialized) {
				if (optAlreadyExisting.isPresent()) {
					LOGGER.info("A ref yet to be materialized is already existing: is this a re-run?");
				} else {
					try {
						GHRef ref = repo.createRef(refName, sha);
						optAlreadyExisting = Optional.of(ref);
						LOGGER.info("We materialized ref={} onto sha1={}", refName, sha);
					} catch (IOException e) {
						// TODO If already exists, should we stop the process, or continue?
						// Another process may be already working on this ref
						throw new UncheckedIOException("Issue materializing ref=" + refName, e);
					}
				}
				String repoName = facade.getRepoFullName();
				refLazyRefCreated.set(new GitRepoBranchSha1(repoName, refName, sha));
			} else if (optAlreadyExisting.isEmpty()) {
				throw new IllegalStateException("The ref has been removed in the meantime: ref=" + refName);
			}

			return GithubDecoratorHelper.decorate(optAlreadyExisting.get());
		};

		return new ILazyGitReference() {

			@Override
			public String getFullRefOrSha1() {
				return sha;
			}

			@Override
			public Supplier<IGitReference> getSupplier() {
				return headSupplier;
			}

		};
	}

	private Optional<GHRef> optRef(GithubRepositoryFacade repo, String refName) {
		return repo.getRefIfPresent(refName);
	}

	private void checkBranchProtection(GHRepository repo, String refName) {
		// TODO Should we refuse, under any circumstances, to write to a baseBranch?
		// Or to any protected branch?
		if (refName.startsWith(CleanthatRefFilterProperties.BRANCHES_PREFIX)) {
			var branchName = refName.substring(CleanthatRefFilterProperties.BRANCHES_PREFIX.length());

			if (branchName.startsWith(ICleanthatGitRefsConstants.REF_DOMAIN_CLEANTHAT_WITH_TRAILING_SLASH)) {
				LOGGER.info(
						"We skip branch-protection validation for cleanthat branches (branch={}),"
								+ " as we are allowed more stuff on them, and they may not exist yet anyway",
						branchName);
			} else {
				GHBranch branch;
				try {
					branch = repo.getBranch(branchName);
				} catch (IOException e) {
					throw new UncheckedIOException("Issue picking branch=" + branchName, e);
				}

				if (branch.isProtected()) {
					// For safety, we prefer not to take the risk of writing onto protected branches, which are any kind
					// of privileged branch
					// This may happen with a PR used to merge some master branch into a custom branch
					// TODO Ensure we discard these scenarios earlier
					throw new IllegalStateException(
							"We should have rejected earlier a scenario leading to write over a protected branch: "
									+ branch);
				}
			}
		}
	}
}
