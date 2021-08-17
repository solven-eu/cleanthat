package eu.solven.cleanthat.it;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import com.google.common.io.Files;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.StandardCharset;

import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookRelevancyResult;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.git_abstraction.GithubFacade;

//https://github-api.kohsuke.org/githubappjwtauth.html
public class ITGithubWebhookHandlerFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITGithubWebhookHandlerFactory.class);

	private static final String SOLVEN_EU_MITRUST_DATASHARING = "solven-eu/mitrust-datasharing";
	private static final String SOLVEN_EU_CLEANTHAT = "solven-eu/cleanthat";
	private static final String SOLVEN_EU_AGILEA = "solven-eu/agilea";
	private static final String SOLVEN_EU_SPRING_BOOT = "solven-eu/spring-boot";

	static PrivateKey get(String filename) throws Exception {
		byte[] keyBytes = Files.toByteArray(new File(filename));
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	@Test
	public void testMakeJwt() throws JOSEException, IOException {
		JWK jwk = JWK.parseFromPEMEncodedObjects(Files.asCharSource(
				new File(System.getProperty("user.home")
						+ "/Dropbox/Solven/Dev/CleanThat/cleanthat.2020-05-19.private-key.pem"),
				StandardCharset.UTF_8).read());

		MockEnvironment env = new MockEnvironment();

		// This is a secret
		LOGGER.debug("github.app.private-jwk: '{}'", jwk.toJSONString());

		// env.setProperty("github.app.app-id", GithubWebhookHandlerFactory.GITHUB_DEFAULT_APP_ID);
		env.setProperty("github.app.private-jwk", jwk.toJSONString());

		GithubWebhookHandlerFactory factory =
				new GithubWebhookHandlerFactory(env, Arrays.asList(ConfigHelpers.makeJsonObjectMapper()));
		IGithubWebhookHandler fresh = factory.makeWithFreshJwt();
		GHApp app = fresh.getGithubAsApp();
		app.listInstallations().forEach(install -> {
			LOGGER.info("appId={} url={}", install.getId(), install.getHtmlUrl());
		});

		GithubAndToken gitHubInstallation = fresh.makeInstallationGithub(9086720);

		// Own repo
		Assertions.assertThat(new GithubFacade(gitHubInstallation.getGithub(), SOLVEN_EU_CLEANTHAT)
				.findFirstPrBaseMatchingRef("refs/heads/master")).isPresent();

		// Private repo in same organisation
		Assertions.assertThat(new GithubFacade(gitHubInstallation.getGithub(), SOLVEN_EU_MITRUST_DATASHARING)
				.findFirstPrBaseMatchingRef("refs/heads/master")).isPresent();

		{
			Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
					.readValue(new ClassPathResource("/github/webhook/pr_open-open_event.json").getInputStream(),
							Map.class);
			GithubWebhookRelevancyResult result =
					new GithubWebhookHandler(app, Arrays.asList(ConfigHelpers.makeJsonObjectMapper()))
							.filterWebhookEventRelevant(new GithubWebhookEvent(body));

			Assertions.assertThat(result.isReviewRequestOpen()).isTrue();
			Assertions.assertThat(result.isPushBranch()).isFalse();
		}
		{
			Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
					.readValue(new ClassPathResource("/github/webhook/pr_open-push_event-1.json").getInputStream(),
							Map.class);
			GithubWebhookRelevancyResult result =
					new GithubWebhookHandler(app, Arrays.asList(ConfigHelpers.makeJsonObjectMapper()))
							.filterWebhookEventRelevant(new GithubWebhookEvent(body));

			Assertions.assertThat(result.isReviewRequestOpen()).isFalse();
			Assertions.assertThat(result.isPushBranch()).isTrue();
		}
		{
			Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
					.readValue(new ClassPathResource("/github/webhook/pr_open-push_event-1.json").getInputStream(),
							Map.class);
			GithubWebhookRelevancyResult result =
					new GithubWebhookHandler(app, Arrays.asList(ConfigHelpers.makeJsonObjectMapper()))
							.filterWebhookEventRelevant(new GithubWebhookEvent(body));

			Assertions.assertThat(result.isReviewRequestOpen()).isFalse();
			Assertions.assertThat(result.isPushBranch()).isTrue();
		}
	}
}
