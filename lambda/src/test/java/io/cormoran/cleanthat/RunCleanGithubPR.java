package io.cormoran.cleanthat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.lambda.CleanThatLambdaFunction;

@SpringBootApplication(scanBasePackages = "none")
public class RunCleanGithubPR extends CleanThatLambdaFunction {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanGithubPR.class);

	private static final String SOLVEN_EU_MITRUST_DATASHARING = "solven-eu/mitrust-datasharing";
	private static final String SOLVEN_EU_CLEANTHAT = "solven-eu/cleanthat";
	private static final String SOLVEN_EU_AGILEA = "solven-eu/agilea";

	final int solvenEuCleanThatInstallationId = 9086720;

	final String repoFullName = SOLVEN_EU_CLEANTHAT;

	public static void main(String[] args) {
		SpringApplication.run(RunCleanGithubPR.class, args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GitHub github = handler.makeInstallationGithub(solvenEuCleanThatInstallationId);

		ObjectMapper objectMapper = new ObjectMapper();
		GithubPullRequestCleaner cleaner = new GithubPullRequestCleaner(objectMapper, new JavaFormatter(objectMapper));

		GHRepository repo;
		try {
			repo = github.getRepository(repoFullName);
		} catch (GHFileNotFoundException e) {
			LOGGER.error("Either the repository is private, or it does not exist: '{}'", repoFullName);
			return;
		}

		LOGGER.info("Repository name={} id={}", repo.getName(), repo.getId());

		String defaultBranchName = Optional.ofNullable(repo.getDefaultBranch()).orElse("master");

		GHBranch defaultBranch;
		try {
			defaultBranch = repo.getBranch(defaultBranchName);
		} catch (GHFileNotFoundException e) {
			throw new IllegalStateException("We can not find as default branch: " + defaultBranchName, e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Optional<Map<String, ?>> defaultBranchConfig = cleaner.defaultBranchConfig(repo, defaultBranch);

		AtomicInteger nbBranchWithConfig = new AtomicInteger();

		repo.queryPullRequests().state(GHIssueState.OPEN).list().forEach(pr -> {
			try {
				Map<String, ?> output = cleaner.formatPR(defaultBranchConfig, nbBranchWithConfig, pr);
				LOGGER.info("Result for {}: {}", pr.getHtmlUrl().toExternalForm(), output);
			} catch (RuntimeException e) {
				LOGGER.warn("Issue processing this PR: " + pr.getHtmlUrl().toExternalForm(), e);
			}
		});

		if (defaultBranchConfig.isEmpty() && nbBranchWithConfig.get() == 0) {
			// At some point, we could prefer remaining silent if we understand the repository tried to integrate us,
			// but did not completed.
			cleaner.openPRWithCleanThatStandardConfiguration(defaultBranch);
		}
	}

}
