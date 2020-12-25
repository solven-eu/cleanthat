package github.it;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.GithubHelper;
import eu.solven.cleanthat.github.GithubSpringConfig;
import eu.solven.cleanthat.github.IStringFormatter;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { GithubSpringConfig.class })
@MockBean({ IStringFormatter.class })
public class ITGithubPullRequestCleaner {
	final GithubPullRequestCleaner cleaner = new GithubPullRequestCleaner(null, null);

	@Autowired
	GithubWebhookHandlerFactory factory;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		IGithubWebhookHandler handler = factory.makeWithFreshJwt();

		GitHub github = handler.getGithubAsApp();
		GHApp app = github.getApp();

		String repoName = "cleanthat";
		GHAppInstallation installation = app.getInstallationByRepository("solven-eu", repoName);
		GitHub githubForRepo = handler.makeInstallationGithub(installation.getAppId());

		cleaner.openPRWithCleanThatStandardConfiguration(githubForRepo,
				GithubHelper.getDefaultBranch(githubForRepo.getRepository(repoName)));
	}
}
