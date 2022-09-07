package eu.solven.cleanthat.it;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import eu.solven.cleanthat.code_provider.github.refs.GithubRefCleaner;
import eu.solven.cleanthat.code_provider.github.refs.all_files.GithubBranchCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.decorator.LazyGitReference;
import eu.solven.cleanthat.formatter.CodeFormatResult;
import eu.solven.cleanthat.github.CleanthatRefFilterProperties;
import eu.solven.cleanthat.github.run.ICleanThatITConstants;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;

/**
 * This enables running CleanThat cleaning logic directly on given branch
 * 
 * @author Benoit Lacelle
 *
 */
public class RunCleanGithubBranch extends ACleanThatXxxApplication implements ICleanThatITConstants {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanGithubBranch.class);

	final String repoFullName = SOLVEN_EU_MITRUST_DATASHARING;
	// If empty, we will process the default branch
	final Optional<String> optBranch = Optional.empty();

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(RunCleanGithubBranch.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();
		GithubWebhookHandlerFactory factory = appContext.getBean(GithubWebhookHandlerFactory.class);
		IGithubWebhookHandler handler = factory.makeWithFreshAuth();

		GithubRefCleaner cleaner = appContext.getBean(GithubRefCleaner.class);
		GHAppInstallation installation = handler.getGithubAsApp()
				.getInstallationByRepository(repoFullName.split("/")[0], repoFullName.split("/")[1]);

		GithubAndToken githubAndToken = handler.makeInstallationGithub(installation.getId());
		GitHub github = githubAndToken.getGithub();

		GHRepository repo;
		try {
			repo = github.getRepository(repoFullName);
		} catch (GHFileNotFoundException e) {
			LOGGER.error("Either the repository is private, or it does not exist: '{}'", repoFullName);
			return;
		}
		LOGGER.info("Repository name={} id={}", repo.getFullName(), repo.getId());
		GHBranch branch = optBranch.map(b -> {
			try {
				return repo.getBranch(b);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).orElseGet(() -> GithubHelper.getDefaultBranch(repo));

		ICodeProvider codeProvider = new GithubBranchCodeProvider(githubAndToken.getToken(), repo, branch);

		CodeProviderHelpers codeProviderHelpers = appContext.getBean(CodeProviderHelpers.class);
		Optional<Map<String, ?>> mainBranchConfig = codeProviderHelpers.unsafeConfig(codeProvider);

		if (mainBranchConfig.isEmpty()) {
			String configureRef = GithubRefCleaner.REF_NAME_CONFIGURE;
			LOGGER.info("CleanThat is not configured in the requested branch ({}). Try switching to {}",
					branch.getName(),
					configureRef);

			branch = repo.getBranch(configureRef);

			ICodeProvider configureBranchCodeProvider =
					new GithubBranchCodeProvider(githubAndToken.getToken(), repo, branch);
			mainBranchConfig = codeProviderHelpers.unsafeConfig(configureBranchCodeProvider);
		}

		if (mainBranchConfig.isEmpty()) {
			behaveOnLackOfConfig(cleaner, githubAndToken, repo, branch, codeProviderHelpers);
		} else {
			doClean(cleaner, repo, branch);
		}
	}

	private void doClean(GithubRefCleaner cleaner, GHRepository repo, GHBranch branch) {
		LOGGER.info("CleanThat is configured in the main/configure branch ({})", branch.getName());

		AtomicReference<GHRef> createdPr = new AtomicReference<>();

		GHBranch finalBranch = branch;
		String refName = CleanthatRefFilterProperties.BRANCHES_PREFIX + finalBranch.getName();
		CodeFormatResult output = cleaner.formatRef(GithubDecoratorHelper.decorate(repo),
				GithubDecoratorHelper.decorate(finalBranch),
				new LazyGitReference(refName, Suppliers.memoize(() -> {
					GHRef pr = GithubHelper.openEmptyRef(repo, finalBranch);
					createdPr.set(pr);
					return GithubDecoratorHelper.decorate(pr);
				})));

		if (createdPr.get() == null) {
			LOGGER.info("Not a single file has been impacted");
		} else {
			LOGGER.info("Created PR: {}", createdPr.get().getUrl().toExternalForm());
			LOGGER.info("Details: {}", output);
		}
	}

	private void behaveOnLackOfConfig(GithubRefCleaner cleaner,
			GithubAndToken githubAndToken,
			GHRepository repo,
			GHBranch branch,
			CodeProviderHelpers codeProviderHelpers) throws IOException {
		LOGGER.info("CleanThat is not configured in the main/configure branch ({})", branch.getName());

		Optional<GHBranch> branchWithConfig = repo.getBranches().values().stream().filter(b -> {
			ICodeProvider configureBranchCodeProvider =
					new GithubBranchCodeProvider(githubAndToken.getToken(), repo, b);
			return codeProviderHelpers.unsafeConfig(configureBranchCodeProvider).isPresent();
		}).findAny();
		boolean configExistsAnywhere = branchWithConfig.isPresent();
		if (configExistsAnywhere) {
			LOGGER.info("There is at least one branch with CleanThat configured ({})",
					branchWithConfig.get().getName());
		} else {
			// At some point, we could prefer remaining silent if we understand the repository tried to integrate
			// us, but did not completed.
			LOGGER.info("About to try condiguring CleanThat in the repo");
			cleaner.tryOpenPRWithCleanThatStandardConfiguration(GithubDecoratorHelper.decorate(branch));
		}
	}
}
