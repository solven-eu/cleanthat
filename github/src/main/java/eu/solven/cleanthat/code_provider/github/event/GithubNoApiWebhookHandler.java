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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.codeprovider.git.GitPrHeadRef;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.github.IGitRefsConstants;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.jvm.GCInspector;

/**
 * Default implementation for IGithubWebhookHandler
 *
 * @author Benoit Lacelle
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads
@SuppressWarnings("PMD.GodClass")
public class GithubNoApiWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubNoApiWebhookHandler.class);

	final List<ObjectMapper> objectMappers;

	public GithubNoApiWebhookHandler(List<ObjectMapper> objectMappers) {
		this.objectMappers = objectMappers;
	}

	/**
	 * Check if a webhook event from Github is processable or not, based on the webhook content exclusively (i.e. not
	 * relying on the API at all)
	 * 
	 * @param githubEvent
	 * @return
	 */
	@SuppressWarnings({ "PMD.ExcessiveMethodLength",
			"checkstyle:MethodLength",
			"PMD.NPathComplexity",
			"PMD.CognitiveComplexity",
			"PMD.ExcessiveMethodLength" })
	// @Override
	public GitWebhookRelevancyResult filterWebhookEventRelevant(I3rdPartyWebhookEvent githubEvent) {
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = githubEvent.getBody();
		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		Optional<Object> organizationUrl = PepperMapHelper.getOptionalAs(input, "organization", "url");
		LOGGER.info("Received a webhook for installationId={} (organization={})",
				installationId,
				organizationUrl.orElse("<missing>"));

		// We are interested in 2 kind of events:
		// PR being (re)open: it is a good time to clean PR-head modified files (if not in a readonly branch)
		// Commit to branches:
		// Either there is a PR associated to the branch: it is relevant to keep the PR clean
		// Or there is no PR associate to the branch, but the branch is to be maintained clean
		// In this later case, we may clean right away on given branch (a bad-practice)
		// Or open a PR cleaning given branch
		// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#push
		// Push on PR: there is no action. There may be multiple commits being pushed
		// Present for PR, PR_review and PR_review_comment
		Optional<Map<String, ?>> optPullRequest = PepperMapHelper.getOptionalAs(input, "pull_request");
		Optional<String> optAction = PepperMapHelper.getOptionalString(input, "action");
		// We are notified a PR has been open: its branch may be keep_cleaned or not
		boolean prOpen;
		Optional<GitPrHeadRef> optOpenPr;
		// We are notified of a commit: its branch may be explicitly keep_cleaned (e.g. master) or implicitly (e.g. it
		// has a PR)
		boolean pushBranch;
		// boolean refHasOpenReviewRequest;
		// baseRef is optional: in case of PR event, it is trivial, but in case of commitPush event, we have to scan for
		// a compatible
		Optional<GitRepoBranchSha1> optBaseRef;
		// If not headRef: this event is not relevant (e.g. it is a comment event)
		Optional<GitRepoBranchSha1> optHeadRef;
		// TODO It is dumb to analyze the event, but we have to do that given we lost the header indicating the type of
		// events through API Gateway and SQS
		if (optPullRequest.isPresent()) {
			pushBranch = false;
			if (optAction.isEmpty()) {
				throw new IllegalStateException("We miss an action for a webhook holding a pull_request");
			}
			String githubAction = optAction.get();
			if ("opened".equals(githubAction) || "reopened".equals(githubAction)) {
				String shortHeadRef = PepperMapHelper.getRequiredString(optPullRequest.get(), "head", "ref");
				String headRef = GithubFacade.branchToRef(shortHeadRef);

				if (headRef.startsWith(GithubRefCleaner.PREFIX_REF_CLEANTHAT)) {
					// Do not process CleanThat own PR open events
					LOGGER.info("We discard as headRef is: {}", headRef);
					return new GitWebhookRelevancyResult(false,
							false, // false,
							Optional.empty(),
							Optional.empty(),
							Optional.empty());
				}
				// Some dirty commits may have been pushed while the PR was closed
				prOpen = true;
				String baseRepoName =
						PepperMapHelper.getRequiredString(optPullRequest.get(), "base", "repo", "full_name");
				String shortBaseRef = PepperMapHelper.getRequiredString(optPullRequest.get(), "base", "ref");
				String baseRef = GithubFacade.branchToRef(shortBaseRef);

				long prNumber = PepperMapHelper.getRequiredNumber(optPullRequest.get(), "number").longValue();
				String headRepoName =
						PepperMapHelper.getRequiredString(optPullRequest.get(), "head", "repo", "full_name");
				String baseSha = PepperMapHelper.getRequiredString(optPullRequest.get(), "base", "sha");
				GitRepoBranchSha1 base = new GitRepoBranchSha1(baseRepoName, baseRef, baseSha);
				optBaseRef = Optional.of(base);
				String headSha = PepperMapHelper.getRequiredString(optPullRequest.get(), "head", "sha");
				GitRepoBranchSha1 head = new GitRepoBranchSha1(headRepoName, headRef, headSha);
				optHeadRef = Optional.of(head);
				optOpenPr = Optional.of(new GitPrHeadRef(baseRepoName, prNumber, base.getRef(), head.getRef()));

				LOGGER.info("Event for repository base={} head={}", baseRepoName, headRepoName);
				LOGGER.info("Event for PR={} {} <- {}", githubAction, base.getRef(), head.getRef());

			} else {
				LOGGER.info("action={}", githubAction);
				prOpen = false;
				// refHasOpenReviewRequest = false;
				optOpenPr = Optional.empty();
				optBaseRef = Optional.empty();
				optHeadRef = Optional.empty();
			}
		} else {
			prOpen = false;
			optOpenPr = Optional.empty();
			if (optAction.isPresent()) {
				LOGGER.info("action={}", optAction.get());
				// Anything but a push
				// i.e. a push event has no action
				pushBranch = false;
				optBaseRef = Optional.empty();
				optHeadRef = Optional.empty();
				// refHasOpenReviewRequest = false;
			} else {
				// 'ref' holds the branch name, but it would lead to issues in case on multiple commits: we prefer to
				// point directly to the sha1. Some codeProvider/events may have events leading to a branch reference,
				// but not a specific sha1
				// TODO Keeping this information may be useful to clean a branch by opening a PR, as the PR has to refer
				// to a branch, not a commit.
				// In fact, keeping only a sha1 is not relevant, as we need a ref/branch to record our cleaning anyway.
				// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#push
				Optional<String> optBeforeSha = PepperMapHelper.getOptionalString(input, "before");
				Optional<String> optAfterSha = PepperMapHelper.getOptionalString(input, "after");
				Optional<String> optFullRefName = PepperMapHelper.getOptionalString(input, "ref");
				if (optAfterSha.isPresent() && optFullRefName.isPresent()) {
					String afterSha = optAfterSha.get();
					if (afterSha.matches("0+")) {
						LOGGER.info("We discard as deleted refs (after={})", afterSha);
						return new GitWebhookRelevancyResult(false,
								false,
								Optional.empty(),
								Optional.empty(),
								Optional.empty());
					}
					String pusherName = PepperMapHelper.getRequiredString(input, "pusher", "name");
					// TODO 'cleanthat' username should not be hardcoded
					if (pusherName.toLowerCase(Locale.US).contains("cleanthat")) {
						LOGGER.info("We discard as pusherName is: {}", pusherName);
						return new GitWebhookRelevancyResult(false,
								false,
								Optional.empty(),
								Optional.empty(),
								Optional.empty());
					}
					pushBranch = true;
					String ref = optFullRefName.get();
					String repoName = PepperMapHelper.getRequiredAs(input, "repository", "full_name");
					LOGGER.info("Event for repository={}", repoName);
					LOGGER.info("Event for pushing into {}", ref);
					GitRepoBranchSha1 after = new GitRepoBranchSha1(repoName, ref, afterSha);
					optHeadRef = Optional.of(after);
					String beforeSha = optBeforeSha.get();

					boolean created = PepperMapHelper.getRequiredBoolean(input, "created");
					boolean forced = PepperMapHelper.getRequiredBoolean(input, "forced");

					if (created) {
						LOGGER.info(
								"This is a ref creation. We may go for a full clean if it was a sensible/cleanable ref");
						optBaseRef = Optional.of(
								new GitRepoBranchSha1(repoName, ref, IGitRefsConstants.SHA1_CLEANTHAT_UP_TO_REF_ROOT));
					} else if (forced) {
						LOGGER.info(
								"This is a forced push. The before sha1 represents before the push, which is not relevant to determine the files to clean");
						optBaseRef = Optional.of(
								new GitRepoBranchSha1(repoName, ref, IGitRefsConstants.SHA1_CLEANTHAT_UP_TO_REF_ROOT));
					} else if (beforeSha.matches("0+")) {
						// 0000000000000000000000000000000000000000
						// AKA z40 is a special reference, meaning no_ref
						// This is typically a branch creation
						// Should we consider as base a parent commit? We can not as a plain branch-creation is not
						// giving any referential
						LOGGER.warn("Branch creation? We consider as base.sha1 the sha1 of after: {}", afterSha);
						optBaseRef = Optional.empty();
					} else {
						// We do not consider as base the RR base, as this is a push event: we are not sure what is the
						// relevant base.
						optBaseRef = Optional.of(new GitRepoBranchSha1(repoName, ref, beforeSha));
					}
				} else {
					// TODO Unclear which case this can be (no pull_request and no action)
					LOGGER.warn("WTF We miss at least one of sha1 and refName");
					pushBranch = false;
					optBaseRef = Optional.empty();
					optHeadRef = Optional.empty();
				}
			}
		}
		// We log the payload temporarily, in order to have easy access to metadata
		if (!GCInspector.inUnitTest()) {
			try {
				LOGGER.debug("TMP payload: {}", ConfigHelpers.getJson(objectMappers).writeValueAsString(input));
			} catch (JsonProcessingException e) {
				LOGGER.warn("Issue while printing the json of the webhook", e);
			}
		}
		return new GitWebhookRelevancyResult(prOpen, pushBranch, optHeadRef, optOpenPr, optBaseRef);
	}

}
