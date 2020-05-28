package io.cormoran.cleanthat;

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

import org.eclipse.jface.text.BadLocationException;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.nimbusds.jose.JOSEException;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.CleanThatRepositoryProperties;
import eu.solven.cleanthat.github.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.IGithubWebhookHandler;
import eu.solven.cleanthat.lambda.CleanThatLambdaFunction;
import io.cormoran.cleanthat.formatter.LineEnding;
import io.cormoran.cleanthat.formatter.eclipse.EclipseJavaFormatter;

@SpringBootApplication
public class RunGithubCleanPR extends CleanThatLambdaFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunGithubCleanPR.class);

	final int solvenEuCleanThatInstallationId = 9086720;

	public static void main(String[] args) {
		SpringApplication.run(RunGithubCleanPR.class, args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GitHub github = handler.makeInstallationGithub(solvenEuCleanThatInstallationId);

		GHRepository repo = github.getRepository("solven-eu/mitrust-datasharing");

		LOGGER.info("Repositry name={} id={}", repo.getName(), repo.getId());

		String defaultBranchName = Optional.ofNullable(repo.getDefaultBranch()).orElse("master");

		GHBranch defaultBranch;
		try {
			defaultBranch = repo.getBranch(defaultBranchName);
		} catch (IOException e) {
			if (e.getMessage().contains("404")) {
				LOGGER.warn("We can not find as default branch: {}", defaultBranchName);
				return;
			} else {
				throw new UncheckedIOException(e);
			}
		}

		ObjectMapper objectMapper = new ObjectMapper();

		Optional<Map<String, ?>> defaultBranchConfig = defaultBranchConfig(repo, defaultBranch, objectMapper);

		AtomicInteger nbBranchWithConfig = new AtomicInteger();

		repo.queryPullRequests().state(GHIssueState.OPEN).list().forEach(pr -> {
			LOGGER.info("PR: {}", pr);

			Optional<Map<String, ?>> prConfig = prConfig(objectMapper, pr);

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
					Optional.ofNullable(PepperMapHelper.<String>getAs(prConfig, "java", "config_url"));
			optConfig.ifPresent(properties::setJavaConfigUrl);

			Set<String> extention = new TreeSet<>();
			pr.listFiles().forEach(file -> {
				String fileName = file.getFilename();

				int lastIndexOfDot = fileName.lastIndexOf('.');

				if (lastIndexOfDot >= 0) {
					extention.add(fileName.substring(lastIndexOfDot + 1));
				}
			});

			if (!extention.contains("java")) {
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
		});

		if (defaultBranchConfig.isEmpty() && nbBranchWithConfig.get() == 0) {
			// At some point, we could prefer remaining silent if we understand the repository tried to integrate us,
			// but did not completed.
			openPRWithCleanThatStandardConfiguration(defaultBranch);
		}
	}

	private Optional<Map<String, ?>> prConfig(ObjectMapper objectMapper, GHPullRequest pr) {
		Optional<Map<String, ?>> prConfig;
		try {
			String asString = loadContent(pr, "/cleanthat.json");

			prConfig = Optional.of(objectMapper.readValue(asString, Map.class));
		} catch (IOException e) {
			if (e.getMessage().contains("404")) {
				LOGGER.info("We miss a '{}' file", "/cleanthat.json");
				prConfig = Optional.empty();
			} else {
				throw new UncheckedIOException(e);
			}
		}
		return prConfig;
	}

	private Optional<Map<String, ?>> defaultBranchConfig(GHRepository repo,
			GHBranch defaultBranch,
			ObjectMapper objectMapper) {
		Optional<Map<String, ?>> defaultBranchConfig;
		try {
			String asString = loadContent(repo, defaultBranch.getSHA1(), "/cleanthat.json");

			defaultBranchConfig = Optional.of(objectMapper.readValue(asString, Map.class));
		} catch (IOException e) {
			if (e.getMessage().contains("404")) {
				LOGGER.info("We miss a '{}' file", "/cleanthat.json");
				defaultBranchConfig = Optional.empty();
			} else {
				throw new UncheckedIOException(e);
			}
		}
		return defaultBranchConfig;
	}

	private void openPRWithCleanThatStandardConfiguration(GHBranch defaultBranch) {
		// Let's follow Renovate and its configuration PR
		// https://github.com/solven-eu/agilea/pull/1
		String body;
		try (InputStreamReader reader =
				new InputStreamReader(new ClassPathResource("/templates/onboarding-body.md").getInputStream(),
						Charsets.UTF_8)) {
			body = CharStreams.toString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		body = body.replaceAll(Pattern.quote("${REPO_FULL_NAME}"), defaultBranch.getOwner().getFullName());

		try {
			defaultBranch.getOwner()
					.createPullRequest("Configure CleanThat",
							"cleanthat/configure",
							defaultBranch.getName(),
							body,
							true,
							false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void formatPR(CleanThatRepositoryProperties properties, GHPullRequest pr) {
		String ref = pr.getHead().getSha();

		GHTreeBuilder createTree = pr.getRepository().createTree();
		AtomicInteger nbFilesInTree = new AtomicInteger();

		pr.listFiles().forEach(file -> {
			if (file.getStatus().equals("removed")) {
				// Skip deleted files
				return;
			}

			String fileName = file.getFilename();
			try {
				String content = loadContent(pr, "cleanthat.json");
			} catch (IOException e) {
				if (e.getMessage().contains("404")) {
					LOGGER.warn("There is no 'cleanthat.json' file");
				} else {
					throw new UncheckedIOException(e);
				}
			}

			int lastIndexOfDot = fileName.lastIndexOf('.');

			if (lastIndexOfDot >= 0 && "java".equals(fileName.substring(lastIndexOfDot + 1))) {
				try {
					String asString = loadContent(pr, file.getFilename());

					String output;
					try {
						output = new EclipseJavaFormatter(properties).doFormat(asString,
								LineEnding.determineLineEnding(asString));
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}

					if (!Strings.isNullOrEmpty(output) && !asString.equals(output)) {
						// TODO isExecutable isn't a parameter from original file?
						createTree.add(file.getFilename(), output, false);
						nbFilesInTree.getAndIncrement();
					}
				} catch (IOException e) {
					throw new UncheckedIOException("Issue with file: " + fileName, e);
				}

				// LineEnding lineEnding = LineEnding.determineLineEnding(file.get);
			}
		});

		if (nbFilesInTree.get() >= 1) {
			LOGGER.info("About to commit {} files into {} ()", nbFilesInTree, pr.getHtmlUrl(), pr.getTitle());
			try {
				GHTree createdTree = createTree.baseTree(ref).create();

				GHCommit commit = pr.getRepository()
						.createCommit()
						.author("CleanThat", "CleanThat", new Date())
						.message("formatting")
						.parent(ref)
						.tree(createdTree.getSha())
						.create();

				String branchRef = pr.getHead().getRef();
				String newHead = commit.getSHA1();
				LOGGER.info("Update ref {} to {}", branchRef, newHead);
				pr.getRepository().getRef("heads/" + branchRef).updateTo(newHead);

				// System.out.println("sha1: " + newHead);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

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