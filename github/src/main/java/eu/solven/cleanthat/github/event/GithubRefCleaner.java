package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.any_language.ACodeCleaner;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.event.pojo.GitRepoBranchSha1;
import eu.solven.cleanthat.github.event.pojo.IExternalWebhookRelevancyResult;

/**
 * Default for {@link IGithubRefCleaner}
 *
 * @author Benoit Lacelle
 */
public class GithubRefCleaner extends ACodeCleaner implements IGithubRefCleaner {

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubRefCleaner.class);

	private static final String REF_DOMAIN_CLEANTHAT = "cleanthat";

	public static final String BRANCH_NAME_CONFIGURE = REF_DOMAIN_CLEANTHAT + "/configure";

	public static final String PREFIX_REF_CLEANTHAT = "refs/heads/" + REF_DOMAIN_CLEANTHAT + "/";

	final GithubAndToken githubAndToken;

	public GithubRefCleaner(List<ObjectMapper> objectMappers,
			ICodeProviderFormatter formatterProvider,
			GithubAndToken githubAndToken) {
		super(objectMappers, formatterProvider);
		this.githubAndToken = githubAndToken;
	}

	// We may have no ref to clean (e.g. there is no cleanthat configuration, or the ref is excluded)
	// We may have to clean current ref (e.g. a PR is open, and we want to clean the PR head)
	// We may have to clean a different ref (e.g. a push to the main branch needs to be cleaned through a PR)
	@Override
	public Optional<String> prepareRefToClean(IExternalWebhookRelevancyResult result,
			GitRepoBranchSha1 theRef,
			Set<String> eventBaseBranches) {
		String refUrl;
		ICodeProvider codeProvider;
		String ref = theRef.getRef();
		try {
			GHRepository repo = githubAndToken.getGithub().getRepository(theRef.getRepoName());
			GHRef refObject = repo.getRef(ref);
			refUrl = refObject.getUrl().toExternalForm();
			codeProvider = new GithubRefCodeProvider(githubAndToken.getToken(), repo, refObject);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Optional<Map<String, ?>> optPrConfig = safeConfig(codeProvider);
		Optional<Map<String, ?>> optConfigurationToUse;
		if (optPrConfig.isEmpty()) {
			LOGGER.info("There is no configuration ({}) on {}", CodeProviderHelpers.PATH_CLEANTHAT, refUrl);
			return Optional.empty();
		} else {
			optConfigurationToUse = optPrConfig;
		}
		Optional<String> version = PepperMapHelper.getOptionalString(optConfigurationToUse.get(), "syntax_version");
		if (version.isEmpty()) {
			LOGGER.warn("No version on configuration applying to PR {}", refUrl);
			return Optional.empty();
		} else if (!"2".equals(version.get())) {
			LOGGER.warn("Version '{}' on configuration is not valid {}", version.get(), refUrl);
			return Optional.empty();
		}
		Map<String, ?> prConfig = optConfigurationToUse.get();
		CleanthatRepositoryProperties properties;
		try {
			properties = prepareConfiguration(prConfig);
		} catch (RuntimeException e) {
			// TODO Send a notification, or open a PR requesting to fix the documentation
			throw new IllegalArgumentException("The configuration file seems invalid");
		}

		// TODO If the configuration changed, trigger full-clean only if the change is an effective change (and not just
		// json/yaml/etc formatting)
		migrateConfigurationCode(properties);
		List<String> cleanableBranchRegexes = properties.getMeta().getRefs().getBranches();

		Optional<String> optBaseMatchingRule = cleanableBranchRegexes.stream().filter(cleanableBranchRegex -> {
			Optional<String> matchingBase = eventBaseBranches.stream().filter(base -> {
				return Pattern.matches(cleanableBranchRegex, base);
			}).findAny();

			if (matchingBase.isEmpty()) {
				LOGGER.info("Not a single base with open RR matches cleanable branch={}", cleanableBranchRegex);
				return false;
			} else {
				LOGGER.info("We have a match for ruleBranch={} eventBaseBranch={}",
						cleanableBranchRegex,
						matchingBase.get());
			}

			return true;
		}).findAny();

		if (optBaseMatchingRule.isPresent()) {
			if (result.isPrOpen()) {
				LOGGER.info("We will clean {} in place as this event is due to a PR (re)open event (rule={})",
						ref,
						optBaseMatchingRule.get());
				return Optional.of(ref);
			} else {
				throw new IllegalStateException("???");
			}
		}

		Optional<String> optHeadMatchingRule = cleanableBranchRegexes.stream().filter(cleanableBranchRegex -> {
			return Pattern.matches(cleanableBranchRegex, ref);
		}).findAny();

		if (optHeadMatchingRule.isPresent()) {
			LOGGER.info(
					"We have an event over a branch which is cleanable, but not head of an open PR to a cleanable base: we shall clean this through a new PR");

			// We never clean inplace: we'll have to open a dedicated ReviewRequest if necessary
			String newBranchRef = PREFIX_REF_CLEANTHAT + UUID.randomUUID();
			// We may open a branch later if it appears this branch is relevant
			// String refToClean = codeProvider.openBranch(ref);
			// BEWARE we do not open the branch right now: we wait to detect at least one fail is relevant to be clean
			// In case of concurrent events, we may end opening multiple PR to clean the same branch
			// TODO Should we handle this specifically when opening the actual branch?
			return Optional.of(newBranchRef);
		} else {
			LOGGER.info("This branch seems not cleanable: {}", ref);
			return Optional.empty();
		}
	}

	@Override
	public Map<String, ?> formatPR(Supplier<GHPullRequest> prSupplier) {
		GHPullRequest pr = prSupplier.get();
		String prUrl = pr.getUrl().toExternalForm();
		// TODO Log if PR is public
		LOGGER.info("PR: {}", prUrl);
		ICodeProviderWriter codeProvider = new GithubPRCodeProvider(githubAndToken.getToken(), pr);
		return formatCodeGivenConfig(codeProvider, false);
	}

	@Override
	public Map<String, ?> formatRefDiff(GHRepository repo, Supplier<GHRef> baseSupplier, Supplier<GHRef> headSupplier) {
		GHRef base = baseSupplier.get();
		GHRef head = headSupplier.get();
		LOGGER.info("Base: {} Head: {}", base.getRef(), head.getRef());
		ICodeProviderWriter codeProvider = new GithubRefDiffCodeProvider(githubAndToken.getToken(), repo, base, head);
		return formatCodeGivenConfig(codeProvider, false);
	}

	@Override
	public Map<String, ?> formatRef(GHRepository repo, Supplier<GHRef> refSupplier) {
		GHRef ref = refSupplier.get();
		String prUrl = ref.getUrl().toExternalForm();
		LOGGER.info("Ref: {}", prUrl);
		ICodeProviderWriter codeProvider = new GithubRefCodeProvider(githubAndToken.getToken(), repo, ref);
		return formatCodeGivenConfig(codeProvider, false);
	}

	public void openPRWithCleanThatStandardConfiguration(GitHub userToServerGithub, GHBranch defaultBranch) {
		GHRepository repo = defaultBranch.getOwner();
		String refName = BRANCH_NAME_CONFIGURE;
		String fullRefName = "refs/heads/" + refName;
		boolean refAlreadyExists;
		Optional<GHRef> refToPR;
		try {
			try {
				refToPR = Optional.of(repo.getRef(fullRefName));
				LOGGER.info("There is already a ref: " + fullRefName);
				refAlreadyExists = true;
			} catch (GHFileNotFoundException e) {
				LOGGER.trace("There is not yet a ref: " + fullRefName, e);
				LOGGER.info("There is not yet a ref: " + fullRefName);
				refAlreadyExists = false;
				refToPR = Optional.empty();
			}
		} catch (IOException e) {
			// TODO If 401, it probably means the Installation is not allowed to see/modify given repository
			throw new UncheckedIOException(e);
		}
		try {
			if (refAlreadyExists) {
				LOGGER.info(
						"There is already a ref about to introduce a cleanthat.json ; do not open a new PR (url={})",
						refToPR.get().getUrl().toExternalForm());
				repo.listPullRequests(GHIssueState.ALL).forEach(pr -> {
					if (refName.equals(pr.getHead().getRef())) {
						LOGGER.info("Related PR: {}", pr.getHtmlUrl());
					}
				});
			} else {
				GHCommit commit = commitConfig(defaultBranch, repo);
				refToPR = Optional.of(repo.createRef(fullRefName, commit.getSHA1()));
				boolean force = false;
				refToPR.get().updateTo(commit.getSHA1(), force);
				// Let's follow Renovate and its configuration PR
				// https://github.com/solven-eu/agilea/pull/1
				String body = readResource("/templates/onboarding-body.md");
				body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), repo.getFullName());
				// Issue using '/' in the base, while renovate succeed naming branches: 'renovate/configure'
				// TODO What is this issue exactly? We seem to success naming our ref 'cleanthat/configure'
				GHPullRequest pr = repo.createPullRequest("Configure CleanThat",
						refToPR.get().getRef(),
						defaultBranch.getName(),
						body,
						true,
						false);
				LOGGER.info("Open PR: {}", pr.getHtmlUrl());
			}
		} catch (IOException e) {
			// TODO If 401, it probably means the Installation is not allowed to modify given repo
			throw new UncheckedIOException(e);
		}
	}

	private GHCommit commitConfig(GHBranch defaultBranch, GHRepository repo) throws IOException {
		// Guess Java version: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L13
		// Detect usage of Checkstyle:
		// https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L35
		// Code formatting: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L17
		// https://github.com/spring-io/spring-javaformat/blob/master/src/checkstyle/checkstyle.xml
		// com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck
		String exampleConfig = readResource("/standard-configurations/standard-java.json");
		GHTree createTree = repo.createTree()
				.baseTree(defaultBranch.getSHA1())
				.add("cleanthat.json", exampleConfig, false)
				.create();
		GHCommit commit = GithubPRCodeProvider.prepareCommit(repo)
				.message("Add cleanthat.json")
				.parent(defaultBranch.getSHA1())
				.tree(createTree.getSha())
				.create();
		return commit;
	}

	private String readResource(String path) {
		String body;
		try (InputStreamReader reader =
				new InputStreamReader(new ClassPathResource(path).getInputStream(), Charsets.UTF_8)) {
			body = CharStreams.toString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return body;
	}
}
