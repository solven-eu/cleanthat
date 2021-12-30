package eu.solven.cleanthat.github.run;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.google.common.base.Suppliers;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.github.GithubHelper;
import eu.solven.cleanthat.code_provider.github.decorator.GithubDecoratorHelper;
import eu.solven.cleanthat.code_provider.github.event.GithubAndToken;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.code_provider.github.refs.GithubBranchCodeProvider;
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;

@Deprecated(since = "DELETEME")
public class RunCleanGithubPullRequest extends ACleanThatXxxApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanGithubPullRequest.class);

	private static final String SOLVEN_EU_MITRUST_DATASHARING = "solven-eu/mitrust-datasharing";

	private static final String SOLVEN_EU_CLEANTHAT = "solven-eu/cleanthat";

	private static final String SOLVEN_EU_AGILEA = "solven-eu/agilea";

	private static final String SOLVEN_EU_SPRING_BOOT = "solven-eu/spring-boot";

	final String repoFullName = SOLVEN_EU_MITRUST_DATASHARING;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(RunCleanGithubPullRequest.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshAuth();

		GHAppInstallation installation = handler.getGithubAsApp()
				.getInstallationByRepository(repoFullName.split("/")[0], repoFullName.split("/")[1]);

		// TODO Unclear when we need an installation/server-to-server Github or a user-to-server Github
		GithubAndToken githubAndToken = handler.makeInstallationGithub(installation.getId());
		GitHub github = githubAndToken.getGithub();
		// GitHub userToServerGithub = handler.getGithubAsApp();

		GithubRefCleaner cleaner = appContext.getBean(GithubRefCleaner.class);
		GHRepository repo;
		try {
			repo = github.getRepository(repoFullName);
		} catch (GHFileNotFoundException e) {
			LOGGER.error("Either the repository is private, or it does not exist: '{}'", repoFullName);
			return;
		}
		LOGGER.info("Repository name={} id={}", repo.getName(), repo.getId());
		GHBranch defaultBranch = GithubHelper.getDefaultBranch(repo);
		GHBranch consideredBranch = defaultBranch;

		ICodeProvider codeProvider = new GithubBranchCodeProvider(githubAndToken.getToken(), repo, defaultBranch);

		CodeProviderHelpers codeProviderHelpers = appContext.getBean(CodeProviderHelpers.class);
		Optional<Map<String, ?>> mainBranchConfig = codeProviderHelpers.unsafeConfig(codeProvider);

		if (mainBranchConfig.isEmpty()) {
			String configureRef = GithubRefCleaner.REF_NAME_CONFIGURE;
			LOGGER.info("CleanThat is not configured in the main branch ({}). Try switching to {}",
					defaultBranch.getName(),
					configureRef);

			consideredBranch = repo.getBranch(configureRef);

			ICodeProvider configureBranchCodeProvider =
					new GithubBranchCodeProvider(githubAndToken.getToken(), repo, consideredBranch);
			mainBranchConfig = codeProviderHelpers.unsafeConfig(configureBranchCodeProvider);
		}

		if (mainBranchConfig.isEmpty()) {
			LOGGER.info("CleanThat is not configured in the main/configure branch ({})", defaultBranch.getName());

			Optional<GHBranch> branchWithConfig = repo.getBranches().values().stream().filter(b -> {
				ICodeProvider configureBranchCodeProvider =
						new GithubBranchCodeProvider(githubAndToken.getToken(), repo, b);
				return codeProviderHelpers.unsafeConfig(configureBranchCodeProvider).isPresent();
			}).findAny();
			boolean configExistsAnywhere = branchWithConfig.isPresent();
			if (!configExistsAnywhere) {
				// At some point, we could prefer remaining silent if we understand the repository tried to integrate
				// us, but did not completed.
				// cleaner.openPRWithCleanThatStandardConfiguration(userToServerGithub, defaultBranch);
			} else {
				LOGGER.info("There is at least one branch with CleanThat configured ({})",
						branchWithConfig.get().getName());
			}
		} else {
			LOGGER.info("CleanThat is configured in the main/configure branch ({})", defaultBranch.getName());

			AtomicReference<GHRef> createdPr = new AtomicReference<>();

			GHBranch finalDefaultBranch = defaultBranch;
			CodeFormatResult output = cleaner.formatRef(GithubDecoratorHelper.decorate(repo), Suppliers.memoize(() -> {
				GHRef pr = GithubHelper.openEmptyRef(repo, finalDefaultBranch);
				createdPr.set(pr);
				return GithubDecoratorHelper.decorate(pr);
			}));

			if (createdPr.get() == null) {
				LOGGER.info("Not a single file has been impacted");
			} else {
				LOGGER.info("Created PR: {}", createdPr.get().getUrl().toExternalForm());
				LOGGER.info("Details: {}", output);
			}
		}
	}
}
