package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitBuilder;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequest;
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

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.CleanThatRepositoryProperties;
import eu.solven.cleanthat.github.IStringFormatter;

/**
 * Default for {@link IGithubPullRequestCleaner}
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubPullRequestCleaner implements IGithubPullRequestCleaner {
	private static final String TEMPLATE_MISS_FILE = "We miss a '{}' file";

	private static final String KEY_JAVA = "java";

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubPullRequestCleaner.class);

	private static final String PATH_CLEANTHAT_JSON = "/cleanthat.json";

	final ObjectMapper objectMapper;
	final IStringFormatter formatter;

	public GithubPullRequestCleaner(ObjectMapper objectMapper, IStringFormatter formatter) {
		this.objectMapper = objectMapper;
		this.formatter = formatter;
	}

	@Override
	public void formatPR(Optional<Map<String, ?>> defaultBranchConfig,
			AtomicInteger nbBranchWithConfig,
			GHPullRequest pr) {
		LOGGER.info("PR: {}", pr);

		Optional<Map<String, ?>> prConfig = prConfig(pr);

		if (prConfig.isEmpty()) {
			if (defaultBranchConfig.isPresent()) {
				LOGGER.info("Config on default branch but not on PR {}", pr.getHtmlUrl().toExternalForm());
			} else {
				LOGGER.info("Config neither on default branch nor on PR {}", pr.getHtmlUrl().toExternalForm());
			}
			return;
		} else {
			nbBranchWithConfig.getAndIncrement();
		}

		CleanThatRepositoryProperties properties = new CleanThatRepositoryProperties();

		Optional<Boolean> optMutatePR =
				Optional.ofNullable(PepperMapHelper.<Boolean>getAs(prConfig, "meta", "mutate_pull_requests"));
		optMutatePR.ifPresent(properties::setAppendToExistingPullRequest);

		Optional<String> optConfig =
				Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "config_url"));
		optConfig.ifPresent(properties::setJavaConfigUrl);

		Optional<String> optJavaEOL = Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, KEY_JAVA, "eol"));
		optJavaEOL.ifPresent(properties::setEol);

		Set<String> extention = new TreeSet<>();
		pr.listFiles().forEach(file -> {
			String fileName = file.getFilename();

			int lastIndexOfDot = fileName.lastIndexOf('.');

			if (lastIndexOfDot >= 0) {
				extention.add(fileName.substring(lastIndexOfDot + 1));
			}
		});

		if (!extention.contains(KEY_JAVA)) {
			LOGGER.info("Not a single .java file impacted by this PR");
			return;
		}

		try {
			GHUser user = pr.getUser();

			// TODO Do not process PR opened by CleanThat
			LOGGER.info("user_id={}", user.getId());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		formatPR(properties, pr);
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
		// Let's follow Renovate and its configuration PR
		// https://github.com/solven-eu/agilea/pull/1
		String body = readResource("/templates/onboarding-body.md");

		GHRepository repo = defaultBranch.getOwner();
		body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), repo.getFullName());

		try {
			GHTree createTree =
					repo.createTree().baseTree(defaultBranch.getSHA1()).add("cleanthat.json", body, false).create();
			GHCommit commit = prepareCommit(repo).message("Add cleanthat.json")
					.parent(defaultBranch.getSHA1())
					.tree(createTree.getSha())
					.create();

			String configureRefName = "refs/heads/cleanthat/configure";
			// AtomicBoolean refAlreadyExists = new AtomicBoolean();

			GHRef refToPR;
			try {
				refToPR = repo.getRef(configureRefName);

				LOGGER.info("There is already a ref: " + configureRefName);
				// refAlreadyExists.set(true);

				boolean force = true;
				refToPR.updateTo(commit.getSHA1(), force);
			} catch (GHFileNotFoundException e) {
				LOGGER.trace("There is not yet a ref: " + configureRefName, e);
				LOGGER.info("There is not yet a ref: " + configureRefName);
				// refAlreadyExists.set(false);
				refToPR = repo.createRef(configureRefName, commit.getSHA1());
			}

			// if (!refAlreadyExists.get()) {
			// }

			// Issue using '/' in the base, while renovate succeed naming branches: 'renovate/configure'
			repo.createPullRequest("Configure CleanThat", refToPR.getRef(), defaultBranch.getName(), body, true, false);
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

	public void formatPR(CleanThatRepositoryProperties properties, GHPullRequest pr) {
		String ref = pr.getHead().getSha();

		GHTreeBuilder createTree = pr.getRepository().createTree();
		AtomicInteger nbFilesInTree = new AtomicInteger();

		pr.listFiles().forEach(file -> {
			if (file.getStatus().equals("removed")) {
				// Skip deleted files
				return;
			}

			String fileName = file.getFilename();

			int lastIndexOfDot = fileName.lastIndexOf('.');

			if (lastIndexOfDot >= 0 && KEY_JAVA.equals(fileName.substring(lastIndexOfDot + 1))) {
				try {
					String asString = loadContent(pr, file.getFilename());

					String output = doFormat(properties, asString);

					if (!Strings.isNullOrEmpty(output) && !asString.equals(output)) {
						// TODO isExecutable isn't a parameter from original file?
						createTree.add(file.getFilename(), output, false);
						nbFilesInTree.getAndIncrement();
					}
				} catch (IOException e) {
					throw new UncheckedIOException("Issue with file: " + fileName, e);
				}
			}
		});

		if (nbFilesInTree.get() >= 1) {
			LOGGER.info("About to commit {} files into {} ({})", nbFilesInTree, pr.getHtmlUrl(), pr.getTitle());
			try {
				GHTree createdTree = createTree.baseTree(ref).create();

				GHCommit commit = prepareCommit(pr.getRepository())
						.message("Formatting " + nbFilesInTree
								.get() + " " + KEY_JAVA + " files with engine=" + properties.getJavaEngine())
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

	}

	private String doFormat(CleanThatRepositoryProperties properties, String asString) throws IOException {
		return formatter.format(properties, asString);
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
