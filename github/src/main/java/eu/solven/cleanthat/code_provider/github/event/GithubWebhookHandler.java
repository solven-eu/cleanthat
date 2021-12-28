package eu.solven.cleanthat.code_provider.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
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
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import cormoran.pepper.collection.PepperMapHelper;
import cormoran.pepper.jvm.GCInspector;
import cormoran.pepper.logging.PepperLogHelper;
import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.pojo.GitPrHeadRef;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookRelevancyResult;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.codeprovider.decorator.IGitReference;
import eu.solven.cleanthat.codeprovider.git.GitRepoBranchSha1;
import eu.solven.cleanthat.codeprovider.git.HeadAndOptionalBase;
import eu.solven.cleanthat.codeprovider.git.IGitRefCleaner;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.git_abstraction.GithubRepositoryFacade;
import eu.solven.cleanthat.lambda.step0_checkwebhook.I3rdPartyWebhookEvent;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.cleanthat.utils.ResultOrError;

/**
 * Default implementation for IGithubWebhookHandler
 *
 * @author Benoit Lacelle
 */
// https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads
@SuppressWarnings("PMD.GodClass")
public class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	private static final String PERMISSION_CHECKS = "checks";

	final GHApp githubApp;

	final List<ObjectMapper> objectMappers;

	public GithubWebhookHandler(GHApp githubApp, List<ObjectMapper> objectMappers) {
		this.githubApp = githubApp;
		this.objectMappers = objectMappers;
	}

	@Override
	public GHApp getGithubAsApp() {
		return githubApp;
	}

	@Override
	public GithubAndToken makeInstallationGithub(long installationId) {
		try {
			GHAppInstallation installationById = getGithubAsApp().getInstallationById(installationId);
			Map<String, GHPermissionType> permissions = installationById.getPermissions();
			LOGGER.info("Permissions: {}", permissions);
			LOGGER.info("RepositorySelection: {}", installationById.getRepositorySelection());
			// https://github.com/hub4j/github-api/issues/570
			// Required to open new pull-requests
			GHAppCreateTokenBuilder installationGithubBuilder = installationById.createToken()
					.permissions(ImmutableMap.<String, GHPermissionType>builder()
							// Required to access a repository without having to list all availablerepositories
							.put("pull_requests", GHPermissionType.WRITE)
							// Required to read files, and commit new versions
							.put("metadata", GHPermissionType.READ)
							// Required to commit cleaned files
							.put("contents", GHPermissionType.WRITE)
							// Required to commit cleaned files
							.put(PERMISSION_CHECKS, GHPermissionType.WRITE)
							.build());
			// https://github.com/hub4j/github-api/issues/570
			String token = installationGithubBuilder.create().getToken();
			GitHub installationGithub = makeInstallationGithub(token);
			// https://stackoverflow.com/questions/45427275/how-to-check-my-github-current-rate-limit
			LOGGER.info("Initialized an installation github. RateLimit status: {}", installationGithub.getRateLimit());
			return new GithubAndToken(installationGithub, token, permissions);
		} catch (GHFileNotFoundException e) {
			throw new UncheckedIOException("Invalid installationId, or no actual access to it?", e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected GitHub makeInstallationGithub(String token) throws IOException {
		return new GitHubBuilder().withAppInstallationToken(token).build();
	}

	@SuppressWarnings({ "PMD.ExcessiveMethodLength",
			"checkstyle:MethodLength",
			"PMD.NPathComplexity",
			"PMD.CognitiveComplexity",
			"PMD.ExcessiveMethodLength" })
	@Override
	public GithubWebhookRelevancyResult filterWebhookEventRelevant(I3rdPartyWebhookEvent githubEvent) {
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = githubEvent.getBody();
		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		Optional<Object> organizationUrl = PepperMapHelper.getOptionalAs(input, "organization", "url");
		LOGGER.info("Received a webhook for installationId={} (organization={})",
				installationId,
				organizationUrl.orElse("-"));
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
		// baseRef is optional: in case of PR even, it is trivial, but in case of commitPush event, we have to scan for
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
				String headRef = PepperMapHelper.getRequiredString(optPullRequest.get(), "head", "ref");
				if (headRef.startsWith(GithubRefCleaner.PREFIX_REF_CLEANTHAT)) {
					// Do not process CleanThat own PR open events
					LOGGER.info("We discard as headRef is: {}", headRef);
					return new GithubWebhookRelevancyResult(false,
							false, // false,
							Optional.empty(),
							Optional.empty(),
							Optional.empty());
				}
				// Some dirty commits may have been pushed while the PR was closed
				prOpen = true;
				// refHasOpenReviewRequest = true;
				String baseRepoName =
						PepperMapHelper.getRequiredString(optPullRequest.get(), "base", "repo", "full_name");
				String baseRef = PepperMapHelper.getRequiredString(optPullRequest.get(), "base", "ref");
				long prNumber = PepperMapHelper.getRequiredNumber(optPullRequest.get(), "number").longValue();
				String headRepoName =
						PepperMapHelper.getRequiredString(optPullRequest.get(), "head", "repo", "full_name");
				String baseSha = PepperMapHelper.getRequiredString(optPullRequest.get(), "base", "sha");
				GitRepoBranchSha1 base = new GitRepoBranchSha1(baseRepoName, baseRef, baseSha);
				optBaseRef = Optional.of(base);
				String headSha = PepperMapHelper.getRequiredString(optPullRequest.get(), "head", "sha");
				GitRepoBranchSha1 head = new GitRepoBranchSha1(headRepoName, headRef, headSha);
				optHeadRef = Optional.of(head);
				optOpenPr = Optional.of(new GitPrHeadRef(baseRepoName,
						prNumber,
						GithubFacade.toFullGitRef(base.getRef()),
						GithubFacade.toFullGitRef(head.getRef())));
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
						return new GithubWebhookRelevancyResult(false,
								false,
								Optional.empty(),
								Optional.empty(),
								Optional.empty());
					}
					String pusherName = PepperMapHelper.getRequiredString(input, "pusher", "name");
					if (pusherName.toLowerCase(Locale.US).contains("cleanthat")) {
						LOGGER.info("We discard as pusherName is: {}", pusherName);
						return new GithubWebhookRelevancyResult(false,
								false,
								Optional.empty(),
								Optional.empty(),
								Optional.empty());
					}
					pushBranch = true;
					String ref = optFullRefName.get();
					String repoName = PepperMapHelper.getRequiredAs(input, "repository", "full_name");
					GitRepoBranchSha1 after = new GitRepoBranchSha1(repoName, ref, afterSha);
					optHeadRef = Optional.of(after);
					String beforeSha = optBeforeSha.get();
					if (beforeSha.matches("0+")) {
						// 0000000000000000000000000000000000000000
						// AKA z40 is a special reference, meaning no_ref
						optBaseRef = Optional.empty();
					} else {
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
		return new GithubWebhookRelevancyResult(prOpen, pushBranch, optHeadRef, optOpenPr, optBaseRef);
	}

	// TODO What if we target a branch which has no configuration, as cleanthat has been introduced in the meantime in
	// the base branch?
	@SuppressWarnings({ "PMD.NPathComplexity", "PMD.CognitiveComplexity", "PMD.ExcessiveMethodLength" })
	@Override
	public WebhookRelevancyResult filterWebhookEventTargetRelevantBranch(ICodeCleanerFactory cleanerFactory,
			IWebhookEvent githubAcceptedEvent) {
		GithubWebhookEvent githubEvent = GithubWebhookEvent.fromCleanThatEvent(githubAcceptedEvent);
		GithubWebhookRelevancyResult offlineResult = filterWebhookEventRelevant(githubEvent);
		if (!offlineResult.isReviewRequestOpen() && !offlineResult.isPushBranch()) {
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = githubEvent.getBody();
		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		GithubAndToken githubAuthAsInst = makeInstallationGithub(installationId);
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
				return WebhookRelevancyResult.dismissed("Installation has hit its own RateLimit. Reset in: " + resetIn);
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
		GitRepoBranchSha1 pushedRefOrRrHead = offlineResult.optPushedRefOrRrHead().get();
		// String repoName = pushedRefOrRrHead.getRepoName();
		GithubRepositoryFacade facade = new GithubRepositoryFacade(baseRepo);
		Optional<GitPrHeadRef> optOpenPr = offlineResult.optOpenPr();
		Set<String> relevantBaseBranches = new TreeSet<>();
		if (offlineResult.isPushBranch()) {
			// This is assumed to be empty as we should not list for RR, before current step
			assert optOpenPr.isEmpty();
			// TODO Is this a valid behavior at all?
			// Why would we impact an open PR with cleaning stuff for the head PR?
			// e.g. a given RR may want to remain neat, and not impacted by a change of configuration in the head
			// WRONG: Here, we are looking for PR merging the pushed branch into some cleanable branch
			// i.e. this is a push to a PR head, we are looking for the PR reference.
			String ref = pushedRefOrRrHead.getRef();
			LOGGER.info("Search for a PR merging the commited branch (head={})", ref);
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
					// GHPullRequest firstRr = prMatchingHead.get(0);
					// LOGGER.info("We spot an open RR with head={}: {}", ref, firstRr.getHtmlUrl());
					// optOpenPr = Optional.of(new GitPrHeadRef(repoName,
					// firstRr.getNumber(),
					// GithubFacade.toFullGitRef(firstRr.getBase()),
					// GithubFacade.toFullGitRef(firstRr.getHead())));
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			assert offlineResult.isReviewRequestOpen();
			assert offlineResult.optOpenPr().isPresent();
			relevantBaseBranches.add(offlineResult.optOpenPr().get().getBaseRef());
		}
		ResultOrError<GitRepoBranchSha1, WebhookRelevancyResult> optTheRef =
				checkRefCleanability(baseRepo, pushedRefOrRrHead, optOpenPr);
		if (optTheRef.getOptError().isPresent()) {
			return optTheRef.getOptError().get();
		}
		GitRepoBranchSha1 dirtyRef = optTheRef.getOptResult().get();
		Optional<String> optSha1 = Optional.of(dirtyRef.getSha());
		if (optSha1.isEmpty()) {
			throw new IllegalStateException("Should not happen");
		}
		if (optSha1.isPresent()) {
			String eventKey = githubEvent.getxGithubDelivery();
			createCheckRun(githubAuthAsInst, baseRepo, optSha1.get(), eventKey);
		}
		IGitRefCleaner cleaner = cleanerFactory.makeCleaner(githubAuthAsInst);
		// BEWARE this branch may not exist: either it is a cleanthat branch yet to create. Or it may be deleted in the
		// meantime (e.g. merged+deleted before cleanthat doing its work)
		Optional<HeadAndOptionalBase> refToClean =
				cleaner.prepareRefToClean(offlineResult, dirtyRef, relevantBaseBranches);
		if (refToClean.isEmpty()) {
			return WebhookRelevancyResult.dismissed(
					"After looking deeper, this event seems not relevant (e.g. no configuration, or forked|readonly head)");
		}
		return WebhookRelevancyResult.relevant(refToClean.get());
	}

	public void createCheckRun(GithubAndToken githubAuthAsInst, GHRepository baseRepo, String sha1, String eventKey) {
		if (GHPermissionType.WRITE == githubAuthAsInst.getPermissions().get(PERMISSION_CHECKS)) {
			// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#check_run
			// https://docs.github.com/en/rest/reference/checks#runs
			// https://docs.github.com/en/rest/reference/permissions-required-for-github-apps#permission-on-checks
			GHCheckRunBuilder checkRunBuilder = baseRepo.createCheckRun("CleanThat", sha1).withExternalID(eventKey);
			try {
				GHCheckRun checkRun = checkRunBuilder.withStatus(Status.IN_PROGRESS).create();

				// We complete right now, until we are able to complete this properly
				checkRun.update().withConclusion(Conclusion.SUCCESS).withStatus(Status.COMPLETED).create();
			} catch (IOException e) {
				// https://github.community/t/resource-not-accessible-when-trying-to-read-write-checkrun/193493
				if (LOGGER.isDebugEnabled()) {
					LOGGER.warn("Issue creating the CheckRun", e);
				} else {
					// As this occurs very often, skip the stacktrace in the logs
					LOGGER.warn("Issue creating the CheckRun");
				}
			}
		} else {
			// Invite users to go into:
			// https://github.com/organizations/solven-eu/settings/installations/9086720
			LOGGER.warn("We are not allowed to write checks (permissions=checks:write)");
		}
	}

	public ResultOrError<GitRepoBranchSha1, WebhookRelevancyResult> checkRefCleanability(GHRepository eventRepo,
			GitRepoBranchSha1 pushedRefOrRrHead,
			Optional<GitPrHeadRef> optOpenPr) {
		// GitRepoBranchSha1 theRef;
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
			if (Long.compare(eventRepo.getId(), prHeadRepository.getId()) == 0) {
				return ResultOrError.error(WebhookRelevancyResult.dismissed(
						"PR in a fork are not managed (as we are not presumably allowed to write in the fork). head="
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
	@Override
	public void doExecuteWebhookEvent(ICodeCleanerFactory cleanerFactory, IWebhookEvent githubAndBranchAcceptedEvent) {
		I3rdPartyWebhookEvent externalCodeEvent = GithubWebhookEvent.fromCleanThatEvent(githubAndBranchAcceptedEvent);
		GithubWebhookRelevancyResult offlineResult = filterWebhookEventRelevant(externalCodeEvent);
		if (!offlineResult.isReviewRequestOpen() && !offlineResult.isPushBranch()) {
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		WebhookRelevancyResult relevancyResult =
				filterWebhookEventTargetRelevantBranch(cleanerFactory, githubAndBranchAcceptedEvent);
		if (relevancyResult.optHeadToClean().isEmpty()) {
			// TODO May happen if the PR is closed in the meantime
			throw new IllegalArgumentException("We should have rejected this earlier");
		}
		// https://developer.github.com/webhooks/event-payloads/
		Map<String, ?> input = externalCodeEvent.getBody();
		long baseRepoId = PepperMapHelper.getRequiredNumber(input, "repository", "id").longValue();
		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		GithubAndToken githubAuthAsInst = makeInstallationGithub(installationId);
		GitHub github = githubAuthAsInst.getGithub();
		GHRepository repo;
		try {
			repo = github.getRepositoryById(baseRepoId);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		IGitRefCleaner cleaner = cleanerFactory.makeCleaner(githubAuthAsInst);
		GithubRepositoryFacade facade = new GithubRepositoryFacade(repo);
		AtomicReference<GitRepoBranchSha1> refLazyRefCreated = new AtomicReference<>();
		// We fetch the head lazily as it may be a Ref to be created lazily, only if there is indeed something to clean
		Supplier<IGitReference> headSupplier = prepareHeadSupplier(relevancyResult, repo, facade, refLazyRefCreated);
		CodeFormatResult result;
		if (relevancyResult.optBaseForHead().isPresent()) {
			// If the base does not exist, then something is wrong: let's check right away it is available
			GHRef base;
			try {
				base = facade.getRef(relevancyResult.optBaseForHead().get().getRef());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			result = cleaner.formatRefDiff(GithubDecoratorHelper.decorate(repo),
					GithubDecoratorHelper.decorate(base),
					headSupplier);
		} else {
			result = cleaner.formatRef(GithubDecoratorHelper.decorate(repo), headSupplier);
		}
		if (refLazyRefCreated.get() != null) {
			GitRepoBranchSha1 lazyRefCreated = refLazyRefCreated.get();
			if (relevancyResult.optBaseForHead().isEmpty()) {
				LOGGER.warn("We created a tmpRef but there is no base");
			} else {
				if (result.isEmpty()) {
					LOGGER.info("Clean is done but no files impacted: the temporary ref ({}) is about to be removed",
							lazyRefCreated);
					try {
						facade.removeRef(lazyRefCreated);
					} catch (IOException e) {
						LOGGER.warn("Issue removing a temporary ref (" + lazyRefCreated + ")", e);
					}
				} else {
					LOGGER.info("Clean is done but and some files are impacted: We open a PR if none already exists");
					doOpenPr(relevancyResult, facade, lazyRefCreated);
				}
			}
		} else {
			LOGGER.debug("The changes would have been committed directly in the head branch");
		}

		logAfterCleaning(installationId, github);
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

	public Supplier<IGitReference> prepareHeadSupplier(WebhookRelevancyResult relevancyResult,
			GHRepository repo,
			GithubRepositoryFacade facade,
			AtomicReference<GitRepoBranchSha1> refLazyRefCreated) {
		Supplier<IGitReference> headSupplier = () -> {
			GitRepoBranchSha1 refToProcess = relevancyResult.optHeadToClean().get();
			String refName = refToProcess.getRef();
			String repoName = repo.getName();
			if (refName.startsWith(GithubRefCleaner.PREFIX_REF_CLEANTHAT_TMPHEAD)) {
				try {
					String sha = refToProcess.getSha();
					repo.createRef(refName, sha);
					LOGGER.info("We created ref={} onto sha1={}", refName, sha);
					refLazyRefCreated.set(new GitRepoBranchSha1(repoName, refName, sha));
				} catch (IOException e) {
					// TODO If already exists, should we stop the process, and continue?
					// Another process may be already working on this ref
					throw new UncheckedIOException("Issue creating ref=" + refName, e);
				}
			}
			try {
				return GithubDecoratorHelper.decorate(facade.getRef(refName));
			} catch (IOException e) {
				throw new UncheckedIOException("Issue fetching ref=" + refName, e);
			}
		};
		return headSupplier;
	}

	public void doOpenPr(WebhookRelevancyResult relevancyResult,
			GithubRepositoryFacade facade,
			GitRepoBranchSha1 lazyRefCreated) {
		// TODO We may want to open a PR in a different repository, in case the original repository does not
		// accept new branches
		Optional<?> optOpenPr;
		GitRepoBranchSha1 base = relevancyResult.optBaseForHead().get();
		try {
			optOpenPr = facade.openPrIfNoneExists(base, lazyRefCreated, "Cleanthat", "Cleanthat <body>");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (optOpenPr.isPresent()) {
			LOGGER.info("We succeeded opening a PR open to merge {} into {}: {}",
					lazyRefCreated,
					base,
					optOpenPr.get());
		} else {
			LOGGER.info("There is already a PR open to merge {} into {}", lazyRefCreated, base);
		}
	}
}
