package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatConfigHelper;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.IStringFormatter;

/**
 * Default for {@link IGithubPullRequestCleaner}
 *
 * @author Benoit Lacelle
 */
public class GithubPullRequestCleaner implements IGithubPullRequestCleaner {

	private static final String KEY_SKIPPED = "skipped";

	private static final String TEMPLATE_MISS_FILE = "We miss a '{}' file";

	// private static final String KEY_JAVA = "java";
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPullRequestCleaner.class);

	public static final String FILENAME_CLEANTHAT_JSON = "cleanthat.json";
	public static final String PATH_CLEANTHAT_JSON = "/" + FILENAME_CLEANTHAT_JSON;

	final ObjectMapper objectMapper;

	final IStringFormatter formatter;

	public GithubPullRequestCleaner(ObjectMapper objectMapper, IStringFormatter formatter) {
		this.objectMapper = objectMapper;
		this.formatter = formatter;
	}

	@Override
	public Map<String, ?> formatPR(CommitContext commitContext, GHPullRequest pr) {
		String prUrl = pr.getHtmlUrl().toExternalForm();
		// TODO Log if PR is public
		LOGGER.info("PR: {}", prUrl);
		Optional<Map<String, ?>> optPrConfig = safePrConfig(pr);
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
		try {
			GHUser user = pr.getUser();
			// TODO Do not process PR opened by CleanThat
			LOGGER.info("user_id={} ({})", user.getId(), user.getHtmlUrl());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
			return formatPR(properties, new GithubPRCodeProvider(pr));
		}
	}

	private CleanthatRepositoryProperties prepareConfiguration(Map<String, ?> prConfig) {
		return CleanthatConfigHelper.parseConfig(objectMapper, prConfig);
	}

	private Optional<Map<String, ?>> safePrConfig(GHPullRequest pr) {
		try {
			return prConfig(pr);
		} catch (RuntimeException e) {
			LOGGER.warn("Issue loading the configuration", e);
			return Optional.empty();
		}
	}

	public Optional<Map<String, ?>> prConfig(GHPullRequest pr) {
		Optional<Map<String, ?>> prConfig;
		try {
			String asString = GithubPRCodeProvider.loadContent(pr, PATH_CLEANTHAT_JSON);
			prConfig = Optional.of(objectMapper.readValue(asString, Map.class));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON, e);
			LOGGER.debug(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON);
			prConfig = Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return prConfig;
	}

	@Override
	public Optional<Map<String, ?>> branchConfig(GHBranch branch) {
		Optional<Map<String, ?>> defaultBranchConfig;
		try {
			String asString =
					GithubPRCodeProvider.loadContent(branch.getOwner(), branch.getSHA1(), PATH_CLEANTHAT_JSON);
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

	public void openPRWithCleanThatStandardConfiguration(GHBranch defaultBranch) {
		GHRepository repo = defaultBranch.getOwner();
		try {
			// Guess Java version: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L13
			// Detect usage of Checkstyle: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L35
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
			String configureRefName = "refs/heads/" + "cleanthat/configure";
			AtomicBoolean refAlreadyExists = new AtomicBoolean();
			GHRef refToPR;
			try {
				refToPR = repo.getRef(configureRefName);
				LOGGER.info("There is already a ref: " + configureRefName);
				refAlreadyExists.set(true);
				boolean force = true;
				refToPR.updateTo(commit.getSHA1(), force);
			} catch (GHFileNotFoundException e) {
				LOGGER.trace("There is not yet a ref: " + configureRefName, e);
				LOGGER.info("There is not yet a ref: " + configureRefName);
				// refAlreadyExists.set(false);
				refToPR = repo.createRef(configureRefName, commit.getSHA1());
			}
			if (refAlreadyExists.get()) {
				LOGGER.info("There is already a ref about to introduce a cleanthat.json ; do not open a new PR");
			} else {
				// Let's follow Renovate and its configuration PR
				// https://github.com/solven-eu/agilea/pull/1
				String body = readResource("/templates/onboarding-body.md");
				body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), repo.getFullName());
				// Issue using '/' in the base, while renovate succeed naming branches: 'renovate/configure'
				repo.createPullRequest("Configure CleanThat",
						refToPR.getRef(),
						defaultBranch.getName(),
						body,
						true,
						false);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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

	public Map<String, ?> formatPR(CleanthatRepositoryProperties properties, ICodeProvider pr) {
		return new CodeProviderFormatter(objectMapper, formatter).formatPR(properties, pr);
	}
}
