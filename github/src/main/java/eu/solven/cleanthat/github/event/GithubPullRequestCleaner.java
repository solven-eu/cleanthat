package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.AtomicLongMap;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.CleanthatConfigHelper;
import eu.solven.cleanthat.github.CleanthatLanguageProperties;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.IStringFormatter;

/**
 * Default for {@link IGithubPullRequestCleaner}
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubPullRequestCleaner implements IGithubPullRequestCleaner {
	private static final String EOL = "\r\n";

	private static final String TEMPLATE_MISS_FILE = "We miss a '{}' file";

	// private static final String KEY_JAVA = "java";

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPullRequestCleaner.class);

	private static final String PATH_CLEANTHAT_JSON = "/cleanthat.json";

	final ObjectMapper objectMapper;
	final IStringFormatter formatter;

	public GithubPullRequestCleaner(ObjectMapper objectMapper, IStringFormatter formatter) {
		this.objectMapper = objectMapper;
		this.formatter = formatter;
	}

	@Override
	public Map<String, ?> formatPR(Optional<Map<String, ?>> defaultBranchConfig,
			AtomicInteger nbBranchWithConfig,
			GHPullRequest pr) {
		String prUrl = pr.getHtmlUrl().toExternalForm();
		// TODO Log if PR is public
		LOGGER.info("PR: {}", prUrl);

		Optional<Map<String, ?>> optPrConfig = safePrConfig(pr);

		if (optPrConfig.isEmpty()) {
			if (defaultBranchConfig.isPresent()) {
				LOGGER.info("Config on default branch but not on PR {}", prUrl);
			} else {
				LOGGER.info("Config neither on default branch nor on PR {}", prUrl);
			}

			return Collections.singletonMap("skipped", "missing '" + PATH_CLEANTHAT_JSON + "'");
		} else {
			nbBranchWithConfig.getAndIncrement();
		}

		try {
			GHUser user = pr.getUser();

			// TODO Do not process PR opened by CleanThat
			LOGGER.info("user_id={} ({})", user.getId(), user.getHtmlUrl());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Map<String, ?> prConfig = optPrConfig.get();
		CleanthatRepositoryProperties properties = prepareConfiguration(prConfig);
		return formatPR(properties, pr);
	}

	private CleanthatRepositoryProperties prepareConfiguration(Map<String, ?> prConfig) {
		return CleanthatConfigHelper.parseConfig(objectMapper, prConfig);
		//
		// OldCleanthatRepositoryProperties properties = new OldCleanthatRepositoryProperties();
		//
		// Optional<Boolean> optMutatePR =
		// Optional.ofNullable(PepperMapHelper.<Boolean>getAs(prConfig, "meta", "mutate_pull_requests"));
		// optMutatePR.ifPresent(properties::setAppendToExistingPullRequest);
		//
		// Optional<String> optConfig =
		// Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "config_url"));
		// optConfig.ifPresent(properties::setJavaConfigUrl);
		//
		// Optional<String> optJavaEOL = Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "eol"));
		// optJavaEOL.ifPresent(properties::setEol);
		//
		// Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "encoding"))
		// .ifPresent(properties::setEncoding);
		//
		// Optional<List<String>> optExcludes =
		// Optional.ofNullable(PepperMapHelper.<List<String>>getAs(prConfig, KEY_JAVA, "excludes"));
		// optExcludes.ifPresent(properties::setExcludes);
		//
		// Optional<List<String>> optIncludes =
		// Optional.ofNullable(PepperMapHelper.<List<String>>getAs(prConfig, KEY_JAVA, "includes"));
		// optIncludes.ifPresent(properties::setIncludes);
		//
		// Optional.ofNullable(PepperMapHelper.<Boolean>getAs(prConfig, KEY_JAVA, "imports", "remove_unused"))
		// .ifPresent(properties::setRemoveUnusedImports);
		//
		// Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "imports", "groups"))
		// .ifPresent(properties::setGroups);
		//
		// Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "imports", "staticGroups"))
		// .ifPresent(properties::setStaticGroups);
		// return properties;
	}

	private boolean fileIsRemoved(GHPullRequestFileDetail file) {
		return file.getStatus().equals("removed");
	}

	// https://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
	private Optional<PathMatcher> findMatching(String fileName, List<String> regex) {
		return regex.stream()
				.map(r -> FileSystems.getDefault().getPathMatcher(r))
				.filter(pm -> pm.matches(Paths.get(fileName)))
				.findFirst();
	}

	private Optional<Map<String, ?>> safePrConfig(GHPullRequest pr) {
		try {
			return prConfig(pr);
		} catch (RuntimeException e) {
			LOGGER.warn("Issue loading the configuration", e);
			return Optional.empty();
		}
	}

	private Optional<Map<String, ?>> prConfig(GHPullRequest pr) {
		Optional<Map<String, ?>> prConfig;
		try {
			String asString = loadContent(pr, PATH_CLEANTHAT_JSON);

			prConfig = Optional.of(objectMapper.readValue(asString, Map.class));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON, e);
			LOGGER.info(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON);
			prConfig = Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return prConfig;
	}

	public Optional<Map<String, ?>> defaultBranchConfig(GHRepository repo, GHBranch defaultBranch) {
		Optional<Map<String, ?>> defaultBranchConfig;
		try {
			String asString = loadContent(repo, defaultBranch.getSHA1(), PATH_CLEANTHAT_JSON);

			defaultBranchConfig = Optional.of(objectMapper.readValue(asString, Map.class));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON, e);
			LOGGER.info(TEMPLATE_MISS_FILE, PATH_CLEANTHAT_JSON);
			defaultBranchConfig = Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return defaultBranchConfig;
	}

	public void openPRWithCleanThatStandardConfiguration(GHBranch defaultBranch) {

		GHRepository repo = defaultBranch.getOwner();

		try {
			String exampleConfig = readResource("/standard-configurations/standard-java.json");
			GHTree createTree = repo.createTree()
					.baseTree(defaultBranch.getSHA1())
					.add("cleanthat.json", exampleConfig, false)
					.create();
			GHCommit commit = prepareCommit(repo).message("Add cleanthat.json")
					.parent(defaultBranch.getSHA1())
					.tree(createTree.getSha())
					.create();

			String configureRefName = "refs/heads/cleanthat/configure";
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

	public Map<String, ?> formatPR(CleanthatRepositoryProperties properties, GHPullRequest pr) {
		String ref = pr.getHead().getSha();

		GHTreeBuilder createTree = pr.getRepository().createTree();

		AtomicLongMap<String> languageToNbAddedFiles = AtomicLongMap.create();

		AtomicLongMap<String> languagesCounters = AtomicLongMap.create();

		List<String> prComments = new ArrayList<>();
		properties.getLanguages().forEach(languageConfig -> {
			String language = PepperMapHelper.getRequiredString(languageConfig, "language");
			ILanguageProperties languageP =
					objectMapper.convertValue(languageConfig, CleanthatLanguageProperties.class);

			LOGGER.info("Applying includes rules: {}", languageP.getIncludes());
			LOGGER.info("Applying excludes rules: {}", languageP.getExcludes());

			// AtomicInteger nbFilesInTree = new AtomicInteger();

			AtomicLongMap<String> languageCounters = AtomicLongMap.create();

			pr.listFiles().forEach(file -> {
				if (fileIsRemoved(file)) {
					// Skip deleted files
					return;
				}

				String fileName = file.getFilename();

				Optional<PathMatcher> matchingInclude = findMatching(fileName, languageP.getIncludes());
				Optional<PathMatcher> matchingExclude = findMatching(fileName, languageP.getExcludes());

				if (matchingInclude.isPresent()) {
					if (matchingExclude.isEmpty()) {
						try {
							String code = loadContent(pr, file.getFilename());

							String output = doFormat(languageP, code);

							// TODO: THIS WONT'T HANDLE MULTIPLE PROCESSORS OVER SAME FILE
							if (!Strings.isNullOrEmpty(output) && !code.equals(output)) {
								// TODO isExecutable isn't a parameter from original file?
								createTree.add(file.getFilename(), output, false);
								languageToNbAddedFiles.incrementAndGet(language);

								languageCounters.incrementAndGet("nb_files_formatted");
							} else {
								languageCounters.incrementAndGet("nb_files_already_formatted");
							}
						} catch (IOException e) {
							throw new UncheckedIOException("Issue with file: " + fileName, e);
						}
					} else {
						languageCounters.incrementAndGet("nb_files_both_included_excluded");
					}
				} else if (matchingExclude.isEmpty()) {
					languageCounters.incrementAndGet("nb_files_excluded_not_included");
				} else {
					languageCounters.incrementAndGet("nb_files_neither_included_nor_included");
				}
			});

			String details = languageCounters.asMap()
					.entrySet()
					.stream()
					.map(e -> e.getKey() + ": " + e.getValue())
					.collect(Collectors.joining(EOL));

			prComments.add("language=" + language + EOL + details);

			languageCounters.asMap().forEach((l, c) -> {
				languagesCounters.addAndGet(l, c);
			});
		});

		if (languageToNbAddedFiles.isEmpty()) {
			LOGGER.info("Not a single file to commit ({})", pr.getHtmlUrl());
		} else {
			LOGGER.info("About to commit {} files into {} ({})",
					languageToNbAddedFiles.sum(),
					pr.getHtmlUrl(),
					pr.getTitle());

			try {
				GHTree createdTree = createTree.baseTree(ref).create();

				String commitMessage = prComments.stream().collect(Collectors.joining(EOL));
				GHCommit commit = prepareCommit(pr.getRepository()).message(commitMessage)
						.parent(ref)
						.tree(createdTree.getSha())
						.create();

				String branchRef = pr.getHead().getRef();
				String newHead = commit.getSHA1();
				LOGGER.info("Update ref {} to {}", branchRef, newHead);
				pr.getRepository().getRef("heads/" + branchRef).updateTo(newHead);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return new LinkedHashMap<>(languagesCounters.asMap());
	}

	private String doFormat(ILanguageProperties properties, String code) throws IOException {
		return formatter.format(properties, code);
	}

	private GHCommitBuilder prepareCommit(GHRepository repo) {
		return repo.createCommit().author("CleanThat", "CleanThat", new Date());
	}

	private String loadContent(GHPullRequest pr, String filename) throws IOException {
		GHRepository repository = pr.getRepository();
		String sha1 = pr.getHead().getSha();
		return loadContent(repository, filename, sha1);
	}

	private String loadContent(GHRepository repository, String filename, String sha1) throws IOException {
		GHContent content = repository.getFileContent(filename, sha1);

		String asString;
		try (InputStreamReader reader = new InputStreamReader(content.read(), Charsets.UTF_8)) {
			asString = CharStreams.toString(reader);
		}
		return asString;
	}
}
