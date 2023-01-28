/*
 * Copyright 2023 Solven
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
package eu.solven.cleanthat.code_provider.github.code_provider;

import com.nimbusds.jose.JOSEException;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubBranchCodeProvider;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import java.io.IOException;
import java.nio.file.FileSystems;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { GithubSpringConfig.class })
@MockBean({ ICodeFormatterApplier.class })
public class ITGithubBranchCodeProvider {
	@Autowired
	GithubWebhookHandlerFactory factory;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		GithubWebhookHandler handler = factory.makeGithubWebhookHandler();

		GHApp app = handler.getGithubAsApp();

		String repoName = "cleanthat";

		GHAppInstallation installation = app.getInstallationByRepository("solven-eu", repoName);
		GithubAndToken githubForRepo = handler.makeInstallationGithub(installation.getId()).getOptResult().get();

		GHRepository cleanthatRepo = githubForRepo.getGithub().getRepository("solven-eu" + "/" + repoName);
		GHBranch masterBranch = cleanthatRepo.getBranch("master");

		GithubSha1CodeProviderHelper codeProvider = new GithubBranchCodeProvider(FileSystems.getDefault(),
				githubForRepo.getToken(),
				cleanthatRepo,
				masterBranch).getHelper();

		// First call: we do clone
		Assert.assertTrue(codeProvider.ensureLocalClone());

		// Second call: already cloned
		Assert.assertFalse(codeProvider.ensureLocalClone());
	}
}
