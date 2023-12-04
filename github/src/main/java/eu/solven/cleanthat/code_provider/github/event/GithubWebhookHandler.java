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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GHMarketplaceAccount;
import org.kohsuke.github.GHMarketplaceAccountPlan;
import org.kohsuke.github.GHMarketplacePlan;
import org.kohsuke.github.GHMarketplacePurchase;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.cleanthat.utils.ResultOrError;
import eu.solven.pepper.collection.PepperMapHelper;
import eu.solven.pepper.logging.PepperLogHelper;

/**
 * Default implementation for IGithubWebhookHandler
 *
 * @author Benoit Lacelle
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads
@SuppressWarnings("PMD.GodClass")
public final class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final String EOL = "\r\n";

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	final IGithubAppFactory githubAppFactory;
	final GHApp githubApp;

	final GithubNoApiWebhookHandler githubNoApiWebhookHandler;
	final GithubCheckRunManager githubCheckRunManager;

	final List<ObjectMapper> objectMappers;

	public GithubWebhookHandler(IGithubAppFactory githubAppFactory,
			List<ObjectMapper> objectMappers,
			GithubCheckRunManager githubCheckRunManager) {
		this.githubAppFactory = githubAppFactory;
		try {
			this.githubApp = githubAppFactory.makeAppGithub().getApp();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		this.githubNoApiWebhookHandler = new GithubNoApiWebhookHandler(objectMappers);
		this.githubCheckRunManager = githubCheckRunManager;

		this.objectMappers = objectMappers;
	}

	@Override
	public GHApp getGithubAsApp() {
		return githubApp;
	}

	/**
	 * 
	 * @param appInstallation
	 * @param ghRepository
	 * @return an {@link Optional} rejection reason
	 * @throws IOException
	 */
	private Optional<String> checkMarketPlacePlan(GHAppInstallation appInstallation, GHRepository ghRepository)
			throws IOException {
		// https://github.com/hub4j/github-api/issues/1613
		GHMarketplaceAccount account = appInstallation.getMarketplaceAccount();
		GHMarketplaceAccountPlan accountPlan = account.getPlan();
		GHMarketplacePurchase purchase = accountPlan.getMarketplacePurchase();
		GHMarketplacePlan plan = purchase.getPlan();

		// https://github.com/marketplace/cleanthat/edit/plans
		if ("retired".equals(plan.getState())) {
			return Optional.of("This plan is retired");
		} else if (!"published".equals(plan.getState())) {
			return Optional.of("Not-managed plan state: " + plan.getState());
		}

		String accountPlanName = plan.getName();
		if (ghRepository.isPrivate()) {
			if (accountPlanName.contains("Private")) {
				LOGGER.info("Accepting an event for a private account");
			} else {
				LOGGER.warn("Private repositories are not accepted by plan={}", accountPlanName);
				return Optional.of("Your plan does not allow private repository");
			}
		}

		return Optional.empty();
	}

	// TODO What if we target a branch which has no configuration, as cleanthat has been introduced in the meantime in
	// the base branch?
	@SuppressWarnings({ "PMD.NPathComplexity", "PMD.CognitiveComplexity", "PMD.ExcessiveMethodLength" })
	// @Override
	public WebhookRelevancyResult filterWebhookEventTargetRelevantBranch(Path root,
			ICodeCleanerFactory cleanerFactory,
			IWebhookEvent githubAcceptedEvent) throws IOException {
		GithubWebhookEvent githubEvent = GithubWebhookEvent.fromCleanThatEvent(githubAcceptedEvent);
		GitWebhookRelevancyResult offlineResult = githubNoApiWebhookHandler.filterWebhookEventRelevant(githubEvent);
		if (!offlineResult.isReviewRequestOpen() && !offlineResult.isPushRef()) {
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = githubEvent.getBody();
		var installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();

		ResultOrError<GithubAndToken, WebhookRelevancyResult> optToken =
				githubAppFactory.makeInstallationGithub(installationId);
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

		GithubCheckRunManager.ifPresent(optCheckRun,
				cr -> cr.update()
						.add(new Output("Branch verification", "Start verification" + "\r\neventKey=" + eventKey))
						.create());

		try {
			{
				Optional<String> optRejectedReason =
						checkMarketPlacePlan(githubAuthAsInst.getGHAppInstallation(), baseRepo);

				if (optRejectedReason.isPresent()) {
					GithubCheckRunManager.ifPresent(optCheckRun,
							c -> c.update()
									.add(new Output("You need to switch to a different market-plan plan",
											optRejectedReason.get() + EOL
													+ "See "
													+ "https://github.com/marketplace/cleanthat/"
													+ EOL
													+ "eventKey="
													+ eventKey))
									.create());

					return WebhookRelevancyResult.dismissed(optRejectedReason.get());
				}
			}

			CheckGithubEventHasValidConfig checkGithubEventHasValidConfig =
					new CheckGithubEventHasValidConfig(root, cleanerFactory);
			WebhookRelevancyResult result = checkGithubEventHasValidConfig
					.doVerifyBranch(offlineResult, githubAuthAsInst, baseRepo, pushedRefOrRrHead, eventKey);

			GithubCheckRunManager.ifPresent(optCheckRun, checkRun -> {
				var optRejectedReason = result.optRejectedReason();

				GHCheckRunBuilder update = checkRun.update();
				if (optRejectedReason.isPresent()) {
					update.withConclusion(Conclusion.NEUTRAL)
							.withStatus(Status.COMPLETED)
							.add(new Output("Rejected due to branch or configuration",
									optRejectedReason.get() + EOL + "eventKey=" + eventKey));
				} else {
					update
							// Not completed as this is to be followed by the cleaning
							.withStatus(Status.IN_PROGRESS)
							.add(new Output("Checking if the branch is valid for cleaning",
									"someSummary" + EOL + "eventKey=" + eventKey));
				}
				update.create();
			});

			return result;
		} catch (

		RuntimeException e) {
			optCheckRun.ifPresent(checkRun -> githubCheckRunManager.reportFailure(checkRun, e));

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
			IWebhookEvent githubAndBranchAcceptedEvent) throws IOException {
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
		GithubAndToken githubAuthAsInst = githubAppFactory.makeInstallationGithub(installationId).getOptResult().get();
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
			optCheckRun.ifPresent(checkRun -> githubCheckRunManager.reportFailure(checkRun, e));

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
