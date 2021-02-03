package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatConfigHelper;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.jgit.CommitContext;

/**
 * Default for {@link IGithubPullRequestCleaner}
 *
 * @author Benoit Lacelle
 */
public class GithubPullRequestCleaner implements IGithubPullRequestCleaner {
	public static final String REF_CONFIGURE = "cleanthat/configure";

	private static final String KEY_SKIPPED = "skipped";

	private static final String TEMPLATE_MISS_FILE = "We miss a '{}' file";

	// private static final String KEY_JAVA = "java";
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPullRequestCleaner.class);

	public static final String FILENAME_CLEANTHAT_JSON = "cleanthat.json";
	public static final String PATH_CLEANTHAT_JSON = "/" + FILENAME_CLEANTHAT_JSON;

	final ObjectMapper objectMapper;

	final ICodeProviderFormatter formatterProvider;

	public GithubPullRequestCleaner(ObjectMapper objectMapper, ICodeProviderFormatter formatterProvider) {
		this.objectMapper = objectMapper;
		this.formatterProvider = formatterProvider;
	}

	@Override
	public Map<String, ?> formatPR(String token, CommitContext commitContext, Supplier<GHPullRequest> prSupplier) {
		GHPullRequest pr = prSupplier.get();

		String prUrl = pr.getUrl().toExternalForm();
		// TODO Log if PR is public
		LOGGER.info("PR: {}", prUrl);

		ICodeProvider codeProvider = new GithubPRCodeProvider(token, pr);

		return formatCodeGivenConfig(commitContext, prUrl, codeProvider);
	}

	@Override
	public Map<String, ?> formatRef(String token,
			CommitContext commitContext,
			GHRepository repo,
			Supplier<GHRef> refSupplier) {
		GHRef ref = refSupplier.get();

		String prUrl = ref.getUrl().toExternalForm();
		LOGGER.info("Ref: {}", prUrl);

		ICodeProvider codeProvider = new GithubRefCodeProvider(token, repo, ref);

		return formatCodeGivenConfig(commitContext, prUrl, codeProvider);
	}

	private Map<String, ?> formatCodeGivenConfig(CommitContext commitContext,
			String prUrl,
			ICodeProvider codeProvider) {
		Optional<Map<String, ?>> optPrConfig = safeConfig(codeProvider);

		Optional<Map<String, ?>> optConfigurationToUse;
		if (optPrConfig.isEmpty()) {
			LOGGER.warn("There is no configuration ({}) on {}", PATH_CLEANTHAT_JSON, prUrl);
			return Collections.singletonMap(KEY_SKIPPED, "missing '" + PATH_CLEANTHAT_JSON + "'");
		} else {
			optConfigurationToUse = optPrConfig;
		}
		Optional<String> version = PepperMapHelper.getOptionalString(optConfigurationToUse.get(), "syntax_version");
		if (version.isEmpty()) {
			LOGGER.info("No version on configuration applying to PR {}", prUrl);
			return Collections.singletonMap(KEY_SKIPPED, "missing 'version' in '" + PATH_CLEANTHAT_JSON + "'");
		} else if (!"2".equals(version.get())) {
			LOGGER.info("Version '{}' on configuration is not valid {}", version.get(), prUrl);
			return Collections.singletonMap(KEY_SKIPPED,
					"Not handled 'version' in '" + PATH_CLEANTHAT_JSON + "' (we accept only '2' for now)");
		}
		Map<String, ?> prConfig = optConfigurationToUse.get();
		CleanthatRepositoryProperties properties = prepareConfiguration(prConfig);
		if (commitContext.isCommitOnMainBranch() && !properties.getMeta().isCleanMainBranch()) {
			LOGGER.info("Skip this commit on main branch as configuration does not allow changes on main branch");
			return Map.of(KEY_SKIPPED, "Commit on main banch, but not allowed to mutate main_branch by configuration");
		} else if (commitContext.isBranchWithoutPR() && properties.getMeta().isCleanOrphanBranches()) {
			LOGGER.info("Skip this commit on main branch as configuration does not allow changes on main branch");
			return Map.of(KEY_SKIPPED,
					"Commit on orphan banch, but not allowed to mutate orphan_branch by configuration");
		} else if (!properties.getMeta().isCleanPullRequests()) {
			return Map.of(KEY_SKIPPED, "Commit on PR, but not allowed to mutate PR by configuration");
		} else {
			return formatCode(properties, codeProvider);
		}
	}

	private CleanthatRepositoryProperties prepareConfiguration(Map<String, ?> prConfig) {
		return CleanthatConfigHelper.parseConfig(objectMapper, prConfig);
	}

	private Optional<Map<String, ?>> safeConfig(ICodeProvider codeProvider) {
		try {
			return unsafeConfig(codeProvider);
		} catch (RuntimeException e) {
			LOGGER.warn("Issue loading the configuration", e);
			return Optional.empty();
		}
	}

	// TODO Get the merged configuration head -> base
	// It will enable cleaning a PR given the configuration of the base branch
	public Optional<Map<String, ?>> unsafeConfig(ICodeProvider codeProvider) {
		Optional<String> optContent;
		try {
			optContent = codeProvider.loadContentForPath(PATH_CLEANTHAT_JSON);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		return optContent.map(content -> {
			try {
				return objectMapper.readValue(content, Map.class);
			} catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Invalid json", e);
			}
		});
	}

	@Override
	public Optional<Map<String, ?>> branchConfig(GHBranch branch) {
		Optional<Map<String, ?>> defaultBranchConfig;
		try {
			String asString =
					GithubPRCodeProvider.loadContent(branch.getOwner(), PATH_CLEANTHAT_JSON, branch.getSHA1());
			defaultBranchConfig = Optional.of(objectMapper.readValue(asString, Map.class));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON, e);
			LOGGER.debug(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON);
			defaultBranchConfig = Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return defaultBranchConfig;
	}

	public void openPRWithCleanThatStandardConfiguration(GitHub userToServerGithub, GHBranch defaultBranch) {
		GHRepository repo = defaultBranch.getOwner();

		String refName = REF_CONFIGURE;
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

	public Map<String, ?> formatCode(CleanthatRepositoryProperties properties, ICodeProvider pr) {
		return formatterProvider.formatCode(properties, pr);
	}
}
