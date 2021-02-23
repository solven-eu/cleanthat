package eu.solven.cleanthat.github.run;

import java.io.IOException;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRateLimit;
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
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.GithubSpringConfig;
import eu.solven.cleanthat.github.ILanguageProperties;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.github.NoWaitRateLimitChecker;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;

@SpringBootApplication(scanBasePackages = "none")
@Import({ GithubSpringConfig.class })
public class RunGithubMonitoring {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunGithubMonitoring.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(RunGithubMonitoring.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	public IStringFormatter stringFormatter() {
		return new IStringFormatter() {

			@Override
			public String format(ILanguageProperties config, String code) throws IOException {
				throw new UnsupportedOperationException("Should not format anything");
			}
		};
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GitHub github = handler.getGithubAsApp();
		GHApp app = github.getApp();
		LOGGER.info("CleanThat has been installed {} times", app.getInstallationsCount());
		app.listInstallations().forEach(installation -> {
			long appId = installation.getAppId();
			// Date creationDate = installation.getCreatedAt();
			String url = installation.getHtmlUrl().toExternalForm();

			try {
				GHAppInstallationToken token = installation.createToken().create();

				GitHub installationGithub = new GitHubBuilder().withAppInstallationToken(token.getToken())
						// .withRateLimitChecker(new NoWaitRateLimitChecker())
						.build();

				GHRateLimit rateLimit = installationGithub.getRateLimit();
				if (0 == rateLimit.getRemaining()) {
					LOGGER.warn("Installation={} has not action left until {}", installation, rateLimit.getResetDate());
					return;
				}

				logAboutUser(installationGithub, installation);
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

	private void logAboutUser(GitHub github, GHAppInstallation installation) {
		GHUser user = installation.getAccount();

		String login = user.getLogin();
		String type = "?";
		try {
			type = user.getType();
		} catch (IOException e) {
			LOGGER.debug("Issue getting type of login=" + login, e);
		}
		LOGGER.info("{}: {}", type, user.getHtmlUrl());

		try {
			unsafeLogUserDetails(user);
		} catch (IOException e) {
			LOGGER.trace("It may not be legit to get User information as an installation. Try as the app");
			try {
				GHUser userAsApp = github.getUser(login);
				unsafeLogUserDetails(userAsApp);
			} catch (IOException e2) {
				try {
					GHOrganization organisationAsApp = github.getOrganization(login);
					unsafeLogUserDetails(organisationAsApp);
				} catch (IOException e3) {
					LOGGER.debug("Issue fetching email for " + type + "=" + login, e3);
				}
			}
		}
	}

	private void unsafeLogUserDetails(GHUser user) throws IOException {
		String email = user.getEmail();
		if (null != email) {
			LOGGER.info("email: {}", email);
		}

		String twitter = user.getTwitterUsername();
		if (null != twitter) {
			LOGGER.info("twitter: {}", twitter);
		}
	}

	private void unsafeLogUserDetails(GHOrganization organisation) throws IOException {
		String email = organisation.getEmail();
		if (null != email) {
			LOGGER.info("email: {}", email);
		}

		String twitter = organisation.getTwitterUsername();
		if (null != twitter) {
			LOGGER.info("twitter: {}", twitter);
		}
	}
}
