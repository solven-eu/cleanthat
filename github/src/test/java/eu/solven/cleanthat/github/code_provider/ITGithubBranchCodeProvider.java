package eu.solven.cleanthat.github.code_provider;

import java.io.IOException;

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

import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.GithubSpringConfig;
import eu.solven.cleanthat.github.event.GithubAndToken;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.github.refs.GithubBranchCodeProvider;
import eu.solven.cleanthat.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.language.ICodeFormatterApplier;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { GithubSpringConfig.class })
@MockBean({ ICodeFormatterApplier.class })
public class ITGithubBranchCodeProvider {
	GithubRefCleaner cleaner;

	@Autowired
	GithubWebhookHandlerFactory factory;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GHApp app = handler.getGithubAsApp();

		String repoName = "cleanthat";

		GHAppInstallation installation = app.getInstallationByRepository("solven-eu", repoName);
		GithubAndToken githubForRepo = handler.makeInstallationGithub(installation.getId());

		GHRepository cleanthatRepo = githubForRepo.getGithub().getRepository("solven-eu" + "/" + repoName);
		GHBranch masterBranch = cleanthatRepo.getBranch("master");

		GithubBranchCodeProvider codeProvider =
				new GithubBranchCodeProvider(githubForRepo.getToken(), cleanthatRepo, masterBranch);

		// First call: we do clone
		Assert.assertTrue(codeProvider.ensureLocalClone());

		// Second call: already cloned
		Assert.assertFalse(codeProvider.ensureLocalClone());
	}
}
