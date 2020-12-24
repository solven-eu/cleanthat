package io.cormoran.cleanthat.local;

import java.io.IOException;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;

@SpringBootApplication(scanBasePackages = "none")
public class RunGithubMonitoring {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunGithubMonitoring.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(RunGithubMonitoring.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env, ObjectMapper objectMapper) {
		return new GithubWebhookHandlerFactory(env, objectMapper);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GitHub github = handler.getGithub();
		GHApp app = github.getApp();
		LOGGER.info("CleanThat has been installed {} times", app.getInstallationsCount());
		app.listInstallations().forEach(installation -> {
			long appId = installation.getAppId();
			// Date creationDate = installation.getCreatedAt();
			String url = installation.getHtmlUrl().toExternalForm();

			GHUser user = installation.getAccount();
			LOGGER.info("user: {}", user.getHtmlUrl());

			try {
				String email = user.getEmail();
				if (null != email) {
					LOGGER.info("email: {}", email);
				}

				String twitter = user.getTwitterUsername();
				if (null != twitter) {
					LOGGER.info("twitter: {}", twitter);
				}
			} catch (IOException e) {
				LOGGER.warn("Issue fetching email for user=" + user, e);
			}

			try {
				GHAppInstallationToken token = installation.createToken().create();

				GitHub installationGithub = new GitHubBuilder().withAppInstallationToken(token.getToken()).build();

				installation.setRoot(installationGithub);

				PagedSearchIterable<GHRepository> repositories = installation.listRepositories();
				LOGGER.info("#repositories: {}", repositories.getTotalCount());
				repositories.forEach(repo -> {
					if (Strings.isNullOrEmpty(repo.getLanguage())) {
						LOGGER.debug("Not very interesting");
					} else {
						LOGGER.info("repo: {}", repo.getHtmlUrl());
						LOGGER.info("language: {}", repo.getLanguage());
					}
				});

				// token.
			} catch (IOException e) {
				LOGGER.warn("Issue generating an installationToken");
			}

			// GHUser user = github.getUser(installation.getAccount().getLogin());

			LOGGER.info("appId={} id={} url={} selection={}",
					appId,
					installation.getId(),
					url,
					installation.getRepositorySelection());
			LOGGER.info("appId={} repositories={}", appId, installation.getRepositoriesUrl());
		});
	}
}
