/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.kohsuke.github.GHApp;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import com.google.common.io.Files;
import com.google.common.jimfs.Jimfs;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.StandardCharset;

import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubCheckRunManager;
import eu.solven.cleanthat.code_provider.github.event.GithubNoApiWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefWriterLogic;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubBranchCodeProvider;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.IGitService;
import eu.solven.cleanthat.git_abstraction.GithubFacade;
import eu.solven.cleanthat.github.IGitRefsConstants;

//https://github-api.kohsuke.org/githubappjwtauth.html
public class ITGithub {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITGithub.class);

	private static final String SOLVEN_EU_MITRUST_DATASHARING = "solven-eu/mitrust-datasharing";
	private static final String SOLVEN_EU_CLEANTHAT = "solven-eu/cleanthat";
	private static final String SOLVEN_EU_CLEANTHAT_ITS = "solven-eu/cleanthat-integrationtests";

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

		GithubWebhookHandlerFactory factory = new GithubWebhookHandlerFactory(env,
				Arrays.asList(ConfigHelpers.makeJsonObjectMapper()),
				new GithubCheckRunManager(Mockito.mock(IGitService.class)));
		GithubNoApiWebhookHandler noauth = factory.makeUnderlyingNoAuth();
		IGithubWebhookHandler fresh = factory.makeGithubWebhookHandler();
		GHApp app = fresh.getGithubAsApp();
		app.listInstallations().forEach(install -> {
			LOGGER.info("appId={} url={}", install.getId(), install.getHtmlUrl());
		});

		GithubAndToken gitHubInstallation = fresh.makeInstallationGithub(9086720).getOptResult().get();

		// Own repo
		GithubFacade ownRepo = new GithubFacade(gitHubInstallation.getGithub(), SOLVEN_EU_CLEANTHAT);
		Assertions.assertThat(ownRepo.findFirstPrBaseMatchingRef("refs/heads/master")).isPresent();

		// Private repo in same organisation
		Assertions.assertThat(new GithubFacade(gitHubInstallation.getGithub(), SOLVEN_EU_MITRUST_DATASHARING)
				.findFirstPrBaseMatchingRef("refs/heads/master")).isPresent();

		{
			Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
					.readValue(new ClassPathResource("/github/webhook/pr_open-push_event-1.json").getInputStream(),
							Map.class);
			GitWebhookRelevancyResult result = noauth.filterWebhookEventRelevant(new GithubWebhookEvent(body));

			Assertions.assertThat(result.isReviewRequestOpen()).isFalse();
			Assertions.assertThat(result.isPushRef()).isTrue();
		}
		{
			Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
					.readValue(new ClassPathResource("/github/webhook/pr_open-push_event-1.json").getInputStream(),
							Map.class);
			GitWebhookRelevancyResult result = noauth.filterWebhookEventRelevant(new GithubWebhookEvent(body));

			Assertions.assertThat(result.isReviewRequestOpen()).isFalse();
			Assertions.assertThat(result.isPushRef()).isTrue();
		}
		{
			GithubFacade itsRepo = new GithubFacade(gitHubInstallation.getGithub(), SOLVEN_EU_CLEANTHAT_ITS);
			GithubRefWriterLogic writer = new GithubRefWriterLogic("ITGithub",
					itsRepo.getRepository(),
					itsRepo.getRef(IGitRefsConstants.BRANCHES_PREFIX + "master"),
					"ITGithub");

			FileSystem fs = Jimfs.newFileSystem();
			GithubBranchCodeProvider codeProvider = new GithubBranchCodeProvider(fs.getPath(fs.getSeparator()),
					gitHubInstallation.getToken(),
					itsRepo.getRepository(),
					itsRepo.getRepository().getBranch("master"));

			Map<Path, String> changes = new LinkedHashMap<>();

			// We write now in any now files
			codeProvider.listFilesForFilenames(file -> {
				if (file.getPath().endsWith("now")) {
					changes.put(file.getPath(), OffsetDateTime.now().toString());
				}
			});

			writer.persistChanges(changes, Arrays.asList("someComment"), Arrays.asList("someLabel"));
		}
	}
}
