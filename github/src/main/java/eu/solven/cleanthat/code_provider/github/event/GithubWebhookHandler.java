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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHMarketplaceAccountPlan;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRef;
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
import com.google.common.base.Ascii;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.decorator.ILazyGitReference;
import eu.solven.cleanthat.codeprovider.git.GitPrHeadRef;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.HeadAndOptionalBase;
import eu.solven.cleanthat.codeprovider.git.IExternalWebhookRelevancyResult;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.config.pojo.CleanthatRefFilterProperties;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.github.ICleanthatGitRefsConstants;
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

	private static final int LIMIT_SUMMARY = 65_535;

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
		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();

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

		GitRepoBranchSha1 pushedRefOrRrHead = offlineResult.optPushedRefOrRrHead().get();
		Optional<GitPrHeadRef> optOpenPr = offlineResult.optOpenPr();

		ResultOrError<GitRepoBranchSha1, WebhookRelevancyResult> optHead =
				checkRefCleanabilityAsHead(baseRepo, pushedRefOrRrHead, optOpenPr);
		if (optHead.getOptError().isPresent()) {
			return optHead.getOptError().get();
		}
		GitRepoBranchSha1 dirtyHeadRef = optHead.getOptResult().get();
		Optional<String> optHeadSha1 = Optional.of(dirtyHeadRef.getSha());
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

			String pushedRef = offlineResult.optBaseRef().get().getRef();
			if (pushedRef.equals(CleanthatRefFilterProperties.BRANCHES_PREFIX + defaultBranch.getName())) {
				LOGGER.debug("About to consider creating a default configuration for {} (as default branch)",
						pushedRef);
				// Open PR with default relevant configuration
				boolean initialized = cleaner.tryOpenPRWithCleanThatStandardConfiguration(root,
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

		// BEWARE this branch may not exist: either it is a cleanthat branch yet to create. Or it may be deleted in the
		// meantime (e.g. merged+deleted before cleanthat doing its work)
		String eventKey = githubEvent.getxGithubDelivery();
		Optional<HeadAndOptionalBase> refToClean =
				cleaner.prepareRefToClean(root, eventKey, offlineResult, dirtyHeadRef, relevantBaseBranches);
		if (refToClean.isEmpty()) {
			return WebhookRelevancyResult.dismissed(
					"After looking deeper, this event seems not relevant (e.g. no configuration, or forked|readonly head)");
		}
		return WebhookRelevancyResult.relevant(refToClean.get());
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
			String ref = pushedRefOrRrHead.getRef();

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
		long baseRepoId = PepperMapHelper.getRequiredNumber(input, "repository", "id").longValue();
		GHRepository baseRepo;
		try {
			baseRepo = githubAsInst.getRepositoryById(baseRepoId);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		LOGGER.info("We are connected to repo: {}", baseRepo.getUrl());
		return ResultOrError.result(baseRepo);
	}

	public ResultOrError<GitRepoBranchSha1, WebhookRelevancyResult> checkRefCleanabilityAsHead(GHRepository eventRepo,
			GitRepoBranchSha1 pushedRefOrRrHead,
			Optional<GitPrHeadRef> optOpenPr) {
		if (optOpenPr.isPresent()) {
			String rawPrNumber = String.valueOf(optOpenPr.get().getId());
			GHPullRequest optPr;
			try {
				int prNumberAsInteger = Integer.parseInt(rawPrNumber);
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
						"PR in a fork are not managed (as we are not presumably allowed to write in the fork). eventRepoId="
								+ eventRepo.getId()
								+ " head="
								+ headRepoFullname));
			}
		}
		String refToClean = pushedRefOrRrHead.getRef();
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
		WebhookRelevancyResult relevancyResult =
				filterWebhookEventTargetRelevantBranch(root, cleanerFactory, githubAndBranchAcceptedEvent);
		if (relevancyResult.optHeadToClean().isEmpty()) {
			// TODO May happen if the PR is closed in the meantime
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = externalCodeEvent.getBody();
		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		GithubAndToken githubAuthAsInst = makeInstallationGithub(installationId).getOptResult().get();
		GHRepository repo = connectToRepository(input, githubAuthAsInst).getOptResult().get();

		String eventKey = ((GithubWebhookEvent) externalCodeEvent).getxGithubDelivery();

		Optional<GHCheckRun> optCheckRun;
		if (offlineResult.isPushRef() && offlineResult.optPushedRefOrRrHead().isPresent()) {
			String sha1 = offlineResult.optPushedRefOrRrHead().get().getSha();
			optCheckRun = githubCheckRunManager.createCheckRun(githubAuthAsInst, repo, sha1, eventKey);
		} else {
			optCheckRun = Optional.empty();
		}

		try {
			IGitRefCleaner cleaner = cleanerFactory.makeCleaner(githubAuthAsInst).get();
			GithubRepositoryFacade facade = new GithubRepositoryFacade(repo);
			AtomicReference<GitRepoBranchSha1> refLazyRefCreated = new AtomicReference<>();
			// We fetch the head lazily as it may be a Ref to be created lazily, only if there is indeed something to
			// commit
			ILazyGitReference headSupplier = prepareHeadSupplier(relevancyResult, repo, facade, refLazyRefCreated);
			CodeFormatResult result =
					GithubEventHelper.executeCleaning(root, relevancyResult, eventKey, cleaner, facade, headSupplier);
			GithubEventHelper.optCreateBranchOpenPr(relevancyResult, facade, refLazyRefCreated, result);

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
				try {
					String stackTrace = Throwables.getStackTraceAsString(e);

					// Summary is limited to 65535 chars
					String summary = Ascii.truncate(stackTrace, LIMIT_SUMMARY, "...");

					checkRun.update()
							.withConclusion(Conclusion.FAILURE)
							.withStatus(Status.COMPLETED)

							.add(new Output(e.getMessage() + " (" + e.getClass().getName() + ")", summary))
							.create();
				} catch (IOException ee) {
					LOGGER.warn("Issue marking the checkRun as completed: " + checkRun.getUrl(), ee);
				}
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

	public ILazyGitReference prepareHeadSupplier(WebhookRelevancyResult relevancyResult,
			GHRepository repo,
			GithubRepositoryFacade facade,
			AtomicReference<GitRepoBranchSha1> refLazyRefCreated) {
		GitRepoBranchSha1 refToProcess = relevancyResult.optHeadToClean().get();
		String refName = refToProcess.getRef();

		checkBranchProtection(repo, refName);

		String sha;

		boolean headIsMaterialized;
		if (refName.startsWith(ICleanthatGitRefsConstants.PREFIX_REF_CLEANTHAT_TMPHEAD)) {
			Optional<GHRef> optAlreadyExisting = optRef(repo, refName);

			if (optAlreadyExisting.isPresent()) {
				sha = optAlreadyExisting.get().getObject().getSha();
				LOGGER.info("The ref {} already exists, with sha1={}", refName, sha);
				headIsMaterialized = true;
			} else {
				sha = refToProcess.getSha();
				headIsMaterialized = false;
			}
		} else {
			sha = refToProcess.getSha();
			headIsMaterialized = false;
		}

		Supplier<IGitReference> headSupplier = () -> {
			String repoName = facade.getRepoFullName();
			if (!headIsMaterialized) {
				Optional<GHRef> optAlreadyExisting = optRef(repo, refName);

				if (!optAlreadyExisting.isPresent()) {
					try {
						repo.createRef(refName, sha);
						LOGGER.info("We materialized ref={} onto sha1={}", refName, sha);
					} catch (IOException e) {
						// TODO If already exists, should we stop the process, or continue?
						// Another process may be already working on this ref
						throw new UncheckedIOException("Issue materializing ref=" + refName, e);
					}
				}
				refLazyRefCreated.set(new GitRepoBranchSha1(repoName, refName, sha));
			}

			try {
				return GithubDecoratorHelper.decorate(facade.getRef(refName));
			} catch (IOException e) {
				throw new UncheckedIOException("Issue fetching ref=" + refName, e);
			}
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

	private Optional<GHRef> optRef(GHRepository repo, String refName) {
		Optional<GHRef> optAlreadyExisting;
		try {
			GHRef ref = repo.getRef(refName);
			optAlreadyExisting = Optional.of(ref);
		} catch (IOException e) {
			LOGGER.info("The ref={} does not exists yet", refName);
			optAlreadyExisting = Optional.empty();
		}
		return optAlreadyExisting;
	}

	private void checkBranchProtection(GHRepository repo, String refName) {
		// TODO Should we refuse, under any circumstances, to write to a baseBranch?
		// Or to any protected branch?
		if (refName.startsWith(CleanthatRefFilterProperties.BRANCHES_PREFIX)) {
			String branchName = refName.substring(CleanthatRefFilterProperties.BRANCHES_PREFIX.length());

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
