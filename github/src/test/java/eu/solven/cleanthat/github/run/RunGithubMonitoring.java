/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.github.run;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GitHubClientUtil;
import org.kohsuke.github.PagedSearchIterable;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.engine.IEngineStep;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.language.IEngineProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * This helps showing insights about installations
 * 
 * @author Benoit Lacelle
 * @see https://github.com/marketplace/cleanthat/insights
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "none")
@Import({ GithubSpringConfig.class })
public class RunGithubMonitoring {

	public static void main(String[] args) {
		new SpringApplicationBuilder(RunGithubMonitoring.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	public IEngineLintFixerFactory stringFormatter() {
		return new IEngineLintFixerFactory() {

			@Override
			public ILintFixer makeLintFixer(CleanthatSession cleanthatSession,
					IEngineProperties engineProperties,
					CleanthatStepProperties stepProperties) {
				throw new UnsupportedOperationException("Should not format anything");
			}

			@Override
			public String getEngine() {
				return "integration_test";
			}

			@Override
			public Set<String> getDefaultIncludes() {
				throw new UnsupportedOperationException();
			}

			@Override
			public CleanthatEngineProperties makeDefaultProperties(Set<String> steps) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Map<String, String> makeCustomDefaultFiles(CleanthatEngineProperties engineProperties,
					Set<String> subStepIds) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<IEngineStep> getMainSteps() {
				return Collections.emptyList();
			}

		};
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		var appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeGithubWebhookHandler();

		GHApp app = handler.getGithubAsApp();
		LOGGER.info("CleanThat has been installed {} times", app.getInstallationsCount());

		// app.getInstallationByOrganization("TheSimpleTeam");
		// app.getInstallationByOrganization("ShaftHQ");

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

				// https://github.com/hub4j/github-api/issues/1082#issuecomment-991289909
				// installation.setRoot(installationGithub);
				// installation.listRepositories()
				PagedSearchIterable<GHRepository> repositories = GitHubClientUtil.listRepositories(installationGithub);
				LOGGER.info("#repositories: {}", repositories.getTotalCount());
				repositories.forEach(repo -> {
					if (Strings.isNullOrEmpty(repo.getLanguage())) {
						LOGGER.debug("Not very interesting");
					} else {
						LOGGER.info("repo: {} (private={})", repo.getHtmlUrl(), repo.isPrivate());
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
		var type = "?";
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
