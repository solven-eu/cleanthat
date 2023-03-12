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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHMarketplaceAccountPlan;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.cleanthat.utils.ResultOrError;
import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.logging.PepperLogHelper;
import okhttp3.OkHttpClient;

/**
 * Default implementation for IGithubWebhookHandler
 *
 * @author Benoit Lacelle
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads
@SuppressWarnings("PMD.GodClass")
public class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	final GithubNoApiWebhookHandler githubNoApiWebhookHandler;
	final GHApp githubApp;
	final GithubCheckRunManager githubCheckRunManager;

	final List<ObjectMapper> objectMappers;

	public GithubWebhookHandler(GHApp githubApp,
			List<ObjectMapper> objectMappers,
			GithubCheckRunManager githubCheckRunManager) {
		this.githubNoApiWebhookHandler = new GithubNoApiWebhookHandler(objectMappers);
		this.githubApp = githubApp;
		this.githubCheckRunManager = githubCheckRunManager;

		this.objectMappers = objectMappers;
	}

	@Override
	public GHApp getGithubAsApp() {
		return githubApp;
	}

	@Override
	public ResultOrError<GithubAndToken, WebhookRelevancyResult> makeInstallationGithub(long installationId) {
		try {
			GHAppInstallation installationById = getGithubAsApp().getInstallationById(installationId);

			// https://github.com/hub4j/github-api/issues/1613
			// GitHub githubRoot = getGithubAsApp().getRoot();
			// checkMarketPlacePlan(installationById, githubRoot);

			Map<String, GHPermissionType> availablePermissions = installationById.getPermissions();

			// This check is dumb, as we should also compare the values
			Map<String, GHPermissionType> requestedPermissions = getRequestedPermissions();
			if (!availablePermissions.keySet().containsAll(requestedPermissions.keySet())) {
				return ResultOrError.error(
						WebhookRelevancyResult.dismissed("We lack proper permissions. Available=" + availablePermissions
								+ " vs requested="
								+ requestedPermissions));
			}

			Map<String, GHPermissionType> permissions = availablePermissions;
			LOGGER.info("Permissions: {}", permissions);
			LOGGER.info("RepositorySelection: {}", installationById.getRepositorySelection());
			// https://github.com/hub4j/github-api/issues/570
			// Required to open new pull-requests
			GHAppCreateTokenBuilder installationGithubBuilder =
					installationById.createToken().permissions(requestedPermissions);

			GHAppInstallationToken installationToken;
			try {
				// https://github.com/hub4j/github-api/issues/570
				installationToken = installationGithubBuilder.create();
			} catch (HttpException e) {
				if (e.getMessage().contains("The permissions requested are not granted to this installation.")) {
					LOGGER.trace("Lack proper permissions", e);
					return ResultOrError.error(WebhookRelevancyResult
							.dismissed("We lack proper permissions. Available=" + availablePermissions));
				} else {
					throw new UncheckedIOException(e);
				}
			}

			String token = installationToken.getToken();
			GitHub installationGithub = makeInstallationGithub(token);

			// https://stackoverflow.com/questions/45427275/how-to-check-my-github-current-rate-limit
			LOGGER.info("Initialized an installation github. RateLimit status: {}", installationGithub.getRateLimit());
			return ResultOrError.result(new GithubAndToken(installationGithub, token, permissions));
		} catch (GHFileNotFoundException e) {
			throw new UncheckedIOException("Invalid installationId, or no actual access to it?", e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void checkMarketPlacePlan(GHAppInstallation installationById, GitHub githubRoot) throws IOException {
		// https://github.com/hub4j/github-api/issues/1613
		githubRoot.listMarketplacePlans().forEach(plan -> {
			// Fetching the list for a single account is inefficient
			List<GHMarketplaceAccountPlan> asList;
			try {
				asList = plan.listAccounts().createRequest().toList();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			GHUser account = installationById.getAccount();
			asList.stream()
					.filter(accountPlan -> 0 == Long.compare(account.getId(), accountPlan.getId()))
					.findAny()
					.ifPresent(accountPlan -> {
						// accountPlan.toString() is seemingly not overriden
						Map<String, Object> accountPlanAsMap = new LinkedHashMap<>();
						accountPlanAsMap.put("id", accountPlan.getId());
						accountPlanAsMap.put("login", accountPlan.getLogin());
						accountPlanAsMap.put("organizationBillingEmail", accountPlan.getOrganizationBillingEmail());
						accountPlanAsMap.put("type", accountPlan.getType());
						accountPlanAsMap.put("url", accountPlan.getUrl());

						LOGGER.info("Account={} is using plan={}", account.getHtmlUrl(), accountPlanAsMap);
					});
		});
	}

	private ImmutableMap<String, GHPermissionType> getRequestedPermissions() {
		return ImmutableMap.<String, GHPermissionType>builder()
				// Required to access a repository without having to list all available repositories
				.put("pull_requests", GHPermissionType.WRITE)
				// Required to read files, and commit new versions
				.put("metadata", GHPermissionType.READ)
				// Required to commit cleaned files
				.put("contents", GHPermissionType.WRITE)
				// Required to edit the checks associated to the cleaning operation
				.put(GithubCheckRunManager.PERMISSION_CHECKS, GHPermissionType.WRITE)
				.build();
	}

	protected GitHub makeInstallationGithub(String token) throws IOException {
		GitHubConnector ghConnector = createGithubConnector();
		return new GitHubBuilder().withAppInstallationToken(token).withConnector(ghConnector).build();
	}

	public static GitHubConnector createGithubConnector() {
		// https://github.com/hub4j/github-api/issues/1202#issuecomment-890362069
		return new OkHttpGitHubConnector(new OkHttpClient());
	}

	// TODO What if we target a branch which has no configuration, as cleanthat has been introduced in the meantime in
	// the base branch?
	@SuppressWarnings({ "PMD.NPathComplexity", "PMD.CognitiveComplexity", "PMD.ExcessiveMethodLength" })
	// @Override
	public WebhookRelevancyResult filterWebhookEventTargetRelevantBranch(Path root,
			ICodeCleanerFactory cleanerFactory,
			IWebhookEvent githubAcceptedEvent) {
		GithubWebhookEvent githubEvent = GithubWebhookEvent.fromCleanThatEvent(githubAcceptedEvent);
		GitWebhookRelevancyResult offlineResult = githubNoApiWebhookHandler.filterWebhookEventRelevant(githubEvent);
		if (!offlineResult.isReviewRequestOpen() && !offlineResult.isPushRef()) {
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = githubEvent.getBody();
		var installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();

		ResultOrError<GithubAndToken, WebhookRelevancyResult> optToken = makeInstallationGithub(installationId);
		if (optToken.getOptError().isPresent()) {
			return optToken.getOptError().get();
		}
		GithubAndToken githubAuthAsInst = optToken.getOptResult().get();
		ResultOrError<GHRepository, WebhookRelevancyResult> baseRepoOrError =
				connectToRepository(input, githubAuthAsInst);

		if (baseRepoOrError.getOptError().isPresent()) {
			return baseRepoOrError.getOptError().get();
		}

		GHRepository baseRepo = baseRepoOrError.getOptResult().get();

		var pushedRefOrRrHead = offlineResult.optPushedRefOrRrHead().get();

		String eventKey = githubEvent.getxGithubDelivery();
		Optional<GHCheckRun> optCheckRun =
				githubCheckRunManager.createCheckRun(githubAuthAsInst, baseRepo, pushedRefOrRrHead.getSha(), eventKey);
		optCheckRun.ifPresent(cr -> {
			try {
				cr.update()
						.add(new Output("Branch verification", "Start verification" + "\r\neventKey=" + eventKey))
						.create();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		try {
			CheckGithubEventHasValidConfig checkGithubEventHasValidConfig =
					new CheckGithubEventHasValidConfig(root, cleanerFactory);
			WebhookRelevancyResult result = checkGithubEventHasValidConfig
					.doVerifyBranch(offlineResult, githubAuthAsInst, baseRepo, pushedRefOrRrHead, eventKey);

			optCheckRun.ifPresent(checkRun -> {
				var optRejectedReason = result.optRejectedReason();

				try {
					if (optRejectedReason.isPresent()) {
						checkRun.update()
								.withConclusion(Conclusion.NEUTRAL)
								.withStatus(Status.COMPLETED)

								.add(new Output("Rejected due to branch or configuration",
										optRejectedReason.get() + "\r\neventKey=" + eventKey))
								.create();
					} else {
						checkRun.update()
								// Not completed as this is to be followed by the cleaning
								.withStatus(Status.IN_PROGRESS)

								.add(new Output("Checking is the branch is valid for cleaning",
										"someSummary" + "\r\neventKey=" + eventKey))
								.create();
					}
				} catch (IOException e) {
					LOGGER.warn("Issue updating CheckRun", e);
				}
			});

			return result;
		} catch (RuntimeException e) {
			optCheckRun.ifPresent(checkRun -> {
				githubCheckRunManager.reportFailure(checkRun, e);
			});

			throw new RuntimeException(e);
		}
	}

	private ResultOrError<GHRepository, WebhookRelevancyResult> connectToRepository(Map<String, ?> input,
			GithubAndToken githubAuthAsInst) {
		GitHub githubAsInst = githubAuthAsInst.getGithub();
		{
			GHRateLimit rateLimit;
			try {
				rateLimit = githubAsInst.getRateLimit();
			} catch (IOException e) {
				throw new UncheckedIOException("Issue checking rateLimit", e);
			}
			int rateLimitRemaining = rateLimit.getRemaining();
			if (rateLimitRemaining == 0) {
				Object resetIn = PepperLogHelper.humanDuration(
						rateLimit.getResetEpochSeconds() * TimeUnit.SECONDS.toMillis(1) - System.currentTimeMillis());
				return ResultOrError.error(WebhookRelevancyResult
						.dismissed("Installation has hit its own RateLimit. Reset in: " + resetIn));
			}
		}
		// We suppose this is always the same as the base repository id
		var baseRepoId = PepperMapHelper.getRequiredNumber(input, "repository", "id").longValue();
		GHRepository baseRepo;
		try {
			baseRepo = githubAsInst.getRepositoryById(baseRepoId);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		LOGGER.info("We are connected to repo: {}", baseRepo.getUrl());
		return ResultOrError.result(baseRepo);
	}

	@SuppressWarnings("PMD.CognitiveComplexity")
	// @Override
	public void doExecuteClean(Path root,
			ICodeCleanerFactory cleanerFactory,
			IWebhookEvent githubAndBranchAcceptedEvent) {
		I3rdPartyWebhookEvent externalCodeEvent = GithubWebhookEvent.fromCleanThatEvent(githubAndBranchAcceptedEvent);
		GitWebhookRelevancyResult offlineResult =
				githubNoApiWebhookHandler.filterWebhookEventRelevant(externalCodeEvent);

		if (!offlineResult.isReviewRequestOpen() && !offlineResult.isPushRef()) {
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		var relevancyResult =
				filterWebhookEventTargetRelevantBranch(root, cleanerFactory, githubAndBranchAcceptedEvent);
		// TODO We may rely on WebhookRelevancyResult.KEY_HEAD_TO_CLEAN instead of recomputing relevancyResult
		if (relevancyResult.optHeadToClean().isEmpty()) {
			// TODO May happen if the PR is closed in the meantime
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = externalCodeEvent.getBody();
		var installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		GithubAndToken githubAuthAsInst = makeInstallationGithub(installationId).getOptResult().get();
		GHRepository repo = connectToRepository(input, githubAuthAsInst).getOptResult().get();

		String eventKey = ((GithubWebhookEvent) externalCodeEvent).getxGithubDelivery();

		Optional<GHCheckRun> optCheckRun;
		var optHead = offlineResult.optPushedRefOrRrHead();
		if (optHead.isPresent()) {
			var sha1 = optHead.get().getSha();
			optCheckRun = githubCheckRunManager.createCheckRun(githubAuthAsInst, repo, sha1, eventKey);
		} else {
			optCheckRun = Optional.empty();
		}

		optCheckRun.ifPresent(checkRun -> {
			try {
				checkRun.update()
						.withStatus(Status.IN_PROGRESS)
						.add(new Output("Cleaning is being executed", "someSummary\r\neventKey=" + eventKey))
						.create();
			} catch (IOException e) {
				LOGGER.warn("Issue marking the checkRun as completed: " + checkRun.getUrl(), e);
			}
		});

		try {
			IGitRefCleaner cleaner = cleanerFactory.makeCleaner(githubAuthAsInst).get();
			// We fetch the head lazily as it may be a Ref to be created lazily, only if there is indeed something to
			// commit
			new GithubCodeCleaner(root, cleaner).executeClean(repo, relevancyResult, eventKey);

			logAfterCleaning(installationId, githubAuthAsInst.getGithub());

			// We complete right now, until we are able to complete this properly
			optCheckRun.ifPresent(checkRun -> {
				try {
					checkRun.update().withConclusion(Conclusion.SUCCESS).withStatus(Status.COMPLETED).create();
				} catch (IOException e) {
					LOGGER.warn("Issue marking the checkRun as completed: " + checkRun.getUrl(), e);
				}
			});
		} catch (RuntimeException e) {
			optCheckRun.ifPresent(checkRun -> {
				githubCheckRunManager.reportFailure(checkRun, e);
			});

			throw new RuntimeException("Propagate", e);
		}
	}

	public void logAfterCleaning(long installationId, GitHub github) {
		try {
			// This is useful to investigate unexpected rateLimitHit
			GHRateLimit rateLimit = github.getRateLimit();
			LOGGER.info("After process, rateLimit={} for installationId={}", rateLimit, installationId);
		} catch (IOException e) {
			LOGGER.warn("Issue with RateLimit", e);
		}
	}

}
