package eu.solven.cleanthat.code_provider.github.code_provider;

import java.io.IOException;

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

import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.language.ICodeFormatterApplier;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { GithubSpringConfig.class })
@MockBean({ ICodeFormatterApplier.class })
public class ITGithubRefCleaner {
	@Autowired
	GithubCodeCleanerFactory cleanerFactory;

	@Autowired
	GithubWebhookHandlerFactory factory;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		IGithubWebhookHandler handler = factory.makeWithFreshAuth();

		GHApp app = handler.getGithubAsApp();

		String repoName = "cleanthat-integrationtests";

		// Ensure the repo is available to the app
		// https://github.com/organizations/solven-eu/settings/installations/9086720
		GHAppInstallation installation = app.getInstallationByRepository("solven-eu", repoName);
		GithubAndToken githubForRepo = handler.makeInstallationGithub(installation.getId());

		GHRepository cleanthatRepo = githubForRepo.getGithub().getRepository("solven-eu" + "/" + repoName);
		GHBranch masterBranch = GithubHelper.getDefaultBranch(cleanthatRepo);

		cleanerFactory.makeCleaner(githubForRepo)
				.get()
				.tryOpenPRWithCleanThatStandardConfiguration(GithubDecoratorHelper.decorate(masterBranch));
	}
}
