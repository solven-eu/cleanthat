package github.it;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
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
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.engine.ICodeFormatterApplier;
import eu.solven.cleanthat.engine.ILanguageLintFixerFactory;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { GithubSpringConfig.class })
@MockBean({ ICodeFormatterApplier.class })
public class ITGithubPullRequestCleaner {
	GithubRefCleaner cleaner;

	@Autowired
	GithubWebhookHandlerFactory factory;

	@Autowired
	List<ObjectMapper> objectMappers;
	@Autowired
	List<ILanguageLintFixerFactory> factories;
	@Autowired
	ICodeProviderFormatter codeProviderFormatter;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		IGithubWebhookHandler handler = factory.makeGithubWebhookHandler();

		GHApp app = handler.getGithubAsApp();

		String repoName = "cleanthat";
		GHAppInstallation installation = app.getInstallationByRepository("solven-eu", repoName);
		GithubAndToken githubForRepo = handler.makeInstallationGithub(installation.getId()).getOptResult().get();

		cleaner = new GithubRefCleaner(objectMappers, factories, codeProviderFormatter, githubForRepo);

		GitHub installationGithub = githubForRepo.getGithub();
		cleaner.tryOpenPRWithCleanThatStandardConfiguration(GithubDecoratorHelper
				.decorate(GithubHelper.getDefaultBranch(installationGithub.getRepository(repoName))));
	}
}
