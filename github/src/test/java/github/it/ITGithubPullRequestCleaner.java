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
package github.it;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubCheckRunManager;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubAppFactory;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.config.ICleanthatConfigInitializer;
import eu.solven.cleanthat.config.IGitService;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { GithubSpringConfig.class })
@MockBean({ ICodeFormatterApplier.class })
public class ITGithubPullRequestCleaner {
	GithubRefCleaner cleaner;

	@Autowired
	IGithubAppFactory ghFactory;
	@Autowired
	GithubWebhookHandlerFactory factory;

	@Autowired
	List<ObjectMapper> objectMappers;
	@Autowired
	ICleanthatConfigInitializer configInitializer;
	@Autowired
	ICodeProviderFormatter codeProviderFormatter;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		var repoName = "cleanthat";
		GHApp ghApp = ghFactory.makeAppGithub().getApp();
		GHAppInstallation installation = ghApp.getInstallationByRepository("solven-eu", repoName);
		GithubAndToken githubForRepo = ghFactory.makeInstallationGithub(installation.getId()).getOptResult().get();

		cleaner = new GithubRefCleaner(objectMappers,
				configInitializer,
				codeProviderFormatter,
				githubForRepo,
				new GithubCheckRunManager(Mockito.mock(IGitService.class)));

		GitHub installationGithub = githubForRepo.getGithub();
		cleaner.tryOpenPRWithCleanThatStandardConfiguration(ITGithubPullRequestCleaner.class.getSimpleName(),
				Files.createTempDirectory("cleanthat-ITGithubPullRequestCleaner-"),
				GithubDecoratorHelper
						.decorate(GithubHelper.getDefaultBranch(installationGithub.getRepository(repoName))));
	}
}
