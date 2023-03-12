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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitPrHeadRef;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.IExternalWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.utils.ResultOrError;

/**
 * Relates to verifying an Event can trigger a cleaning or not
 * 
 * @author Benoit Lacelle
 *
 */
public class CheckGithubEventHasValidConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckGithubEventHasValidConfig.class);

	final Path root;
	final ICodeCleanerFactory cleanerFactory;

	public CheckGithubEventHasValidConfig(Path root, ICodeCleanerFactory cleanerFactory) {
		this.root = root;
		this.cleanerFactory = cleanerFactory;
	}

	public WebhookRelevancyResult doVerifyBranch(GitWebhookRelevancyResult offlineResult,
			GithubAndToken githubAuthAsInst,
			GHRepository baseRepo,
			GitRepoBranchSha1 pushedRefOrRrHead,
			String eventKey) {
		var optOpenPr = offlineResult.optOpenPr();

		ResultOrError<GitRepoBranchSha1, WebhookRelevancyResult> optHead =
				checkRefCleanabilityAsHead(baseRepo, pushedRefOrRrHead, optOpenPr);
		if (optHead.getOptError().isPresent()) {
			return optHead.getOptError().get();
		}
		var dirtyHeadRef = optHead.getOptResult().get();
		var optHeadSha1 = Optional.of(dirtyHeadRef.getSha());
		if (optHeadSha1.isEmpty()) {
			throw new IllegalStateException("Should not happen");
		}

		IGitRefCleaner cleaner = cleanerFactory.makeCleaner(githubAuthAsInst).get();

		// We rely on push over branches to trigger initialization
		if (offlineResult.isPushRef()) {
			GHBranch defaultBranch;
			try {
				defaultBranch = GithubHelper.getDefaultBranch(baseRepo);
			} catch (RuntimeException e) {
				LOGGER.warn("We failed finding the default branch", e);
				return WebhookRelevancyResult.dismissed("Issue guessing the default branch");
			}

			var pushedRef = offlineResult.optBaseRef().get().getRef();
			if (pushedRef.equals(CleanthatRefFilterProperties.BRANCHES_PREFIX + defaultBranch.getName())) {
				LOGGER.debug("About to consider creating a default configuration for {} (as default branch)",
						pushedRef);
				// Open PR with default relevant configuration
				boolean initialized = cleaner.tryOpenPRWithCleanThatStandardConfiguration(eventKey,
						root,
						GithubDecoratorHelper.decorate(defaultBranch));

				if (initialized) {
					return WebhookRelevancyResult.dismissed("We just open a PR with default configuration");
				}
			} else {
				LOGGER.debug("This is not a push over the default branch ({}): {}", defaultBranch.getName(), pushedRef);
			}
		}

		GithubRepositoryFacade facade = new GithubRepositoryFacade(baseRepo);
		Set<String> relevantBaseBranches =
				computeRelevantBaseBranches(offlineResult, pushedRefOrRrHead, facade, optOpenPr);

		// BEWARE this branch may not exist: either it is a cleanthat branch yet to create. Or it may be deleted in
		// the meantime (e.g. merged+deleted before cleanthat doing its work)
		var refToClean = cleaner.prepareRefToClean(root, eventKey, offlineResult, dirtyHeadRef, relevantBaseBranches);
		if (refToClean.isEmpty()) {
			return WebhookRelevancyResult.dismissed(
					"After looking deeper, this event seems not relevant (e.g. no configuration, or forked|readonly head)");
		}
		return WebhookRelevancyResult.relevant(refToClean.get());
	}

	public ResultOrError<GitRepoBranchSha1, WebhookRelevancyResult> checkRefCleanabilityAsHead(GHRepository eventRepo,
			GitRepoBranchSha1 pushedRefOrRrHead,
			Optional<GitPrHeadRef> optOpenPr) {
		if (optOpenPr.isPresent()) {
			var rawPrNumber = String.valueOf(optOpenPr.get().getId());
			GHPullRequest optPr;
			try {
				var prNumberAsInteger = Integer.parseInt(rawPrNumber);
				optPr = eventRepo.getPullRequest(prNumberAsInteger);
			} catch (GHFileNotFoundException e) {
				LOGGER.debug("PR does not exists. Closed?", e);
				LOGGER.warn("PR={} does not exists. Closed?", rawPrNumber);
				return ResultOrError
						.error(WebhookRelevancyResult.dismissed("PR does not exists. Closed? pr=" + rawPrNumber));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			GHCommitPointer prHead = optPr.getHead();
			GHRepository prHeadRepository = prHead.getRepository();
			String headRepoFullname = prHeadRepository.getFullName();
			// We rely on Long.compare to workaround PMD complaining about == used over Strings
			// which is seemingly an issue with @WithBridgeMethods
			if (Long.compare(eventRepo.getId(), prHeadRepository.getId()) != 0) {
				// https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/
				// working-with-forks/allowing-changes-to-a-pull-request-branch-created-from-a-fork
				return ResultOrError.error(WebhookRelevancyResult.dismissed(
						"PR with head in a fork (as we are not presumably allowed to write in the fork). eventRepoId="
								+ eventRepo.getId()
								+ " head="
								+ headRepoFullname));
			}
		}
		var refToClean = pushedRefOrRrHead.getRef();
		try {
			new GithubRepositoryFacade(eventRepo).getRef(refToClean);
		} catch (GHFileNotFoundException e) {
			LOGGER.debug("Ref does not exists. Deleted?", e);
			LOGGER.warn("Ref does not exists. Deleted?={} does not exists. Deleted?", refToClean);
			return ResultOrError
					.error(WebhookRelevancyResult.dismissed("Ref does not exists. Deleted? ref=" + refToClean));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return ResultOrError.result(pushedRefOrRrHead);
	}

	/**
	 * 
	 * @param offlineResult
	 * @param pushedRefOrRrHead
	 * @param facade
	 * @param optOpenPr
	 * @return in case of push events, we return all refs used as base of a RR, where the pushed branch in the head.
	 * 
	 *         in case of a RR event, we return only the RR base
	 */
	private Set<String> computeRelevantBaseBranches(IExternalWebhookRelevancyResult offlineResult,
			GitRepoBranchSha1 pushedRefOrRrHead,
			GithubRepositoryFacade facade,
			Optional<GitPrHeadRef> optOpenPr) {
		Set<String> relevantBaseBranches = new TreeSet<>();
		if (offlineResult.isPushRef()) {
			// This is assumed to be empty as we should not list for RR, before current step
			assert optOpenPr.isEmpty();
			// TODO Is this a valid behavior at all?
			// Why would we impact an open PR with cleaning stuff for the head PR?
			// e.g. a given RR may want to remain neat, and not impacted by a change of configuration in the head
			// WRONG: Here, we are looking for PR merging the pushed branch into some cleanable branch
			// i.e. this is a push to a PR head, we are looking for the PR reference.
			var ref = pushedRefOrRrHead.getRef();

			LOGGER.info("Search for a PR with head the commited branch (head={})", ref);
			try {
				List<GHPullRequest> prMatchingHead = facade.findAnyPrHeadMatchingRef(ref).collect(Collectors.toList());
				if (prMatchingHead.isEmpty()) {
					LOGGER.info("There is no open RR with head={}", ref);
				} else {
					prMatchingHead.forEach(pr -> {
						relevantBaseBranches.add(GithubFacade.toFullGitRef(pr.getBase()));
					});
					// There is no point in forcing to get a RR, as this is used later only to check the RR is still
					// open, or to append a comment: given N compatible RR< none should be impacted on a push event
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			assert offlineResult.isReviewRequestOpen();
			assert offlineResult.optOpenPr().isPresent();

			// WARNING we have reason to believe we should clean all files in the RR (i.e. as base the RR base, not the
			// before of the push)
			// Especially in case of ref-creation, with which we may not clean anything (not knowing yet the proper
			// base)
			relevantBaseBranches.add(offlineResult.optOpenPr().get().getBaseRef());
		}
		return relevantBaseBranches;
	}

}
