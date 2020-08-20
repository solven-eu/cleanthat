package io.cormoran.cleanthat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.github.GHApp;
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

import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.lambda.CleanThatLambdaFunction;
import io.cormoran.cleanthat.formatter.eclipse.JavaFormatter;

@SpringBootApplication
public class RunGithubMonitoring extends CleanThatLambdaFunction {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunGithubMonitoring.class);

	public static void main(String[] args) {
		SpringApplication.run(RunGithubMonitoring.class, args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GHApp app = handler.getGithub().getApp();
		LOGGER.info("CleanThat has been installed {} times", app.getInstallationsCount());

		app.listInstallations().forEach(installation -> {
			long appId = installation.getAppId();
			// Date creationDate = installation.getCreatedAt();
			String url = installation.getHtmlUrl().toExternalForm();

			LOGGER.info("appId={} url={} selection={}", appId, url, installation.getRepositorySelection());
		});
	}

}
