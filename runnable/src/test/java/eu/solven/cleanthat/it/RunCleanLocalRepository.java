package eu.solven.cleanthat.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;

/**
 * This enables easy cleaning of any given folder. Given folder is supposedly the root of a repository
 * 
 * 
 * @author Benoit Lacelle
 *
 */
public class RunCleanLocalRepository extends ACleanThatXxxApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanLocalRepository.class);

	public static void main(String[] args) {
		SpringApplication springApp = new SpringApplication(RunCleanLocalRepository.class);

		springApp.setWebApplicationType(WebApplicationType.NONE);

		springApp.run(args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		// One can adjust this to any local folder
		Path repoFolder = Paths.get(System.getProperty("user.home"), "workspace3", "mitrust-datasharing");

		LOGGER.info("About to process {}", repoFolder);

		ICodeProviderWriter codeProvider = makeCodeProvider(repoFolder);

		ApplicationContext appContext = event.getApplicationContext();
		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		File pathToConfig = CodeProviderHelpers.pathToConfig(repoFolder);

		ConfigHelpers configHelper = new ConfigHelpers(appContext.getBeansOfType(ObjectMapper.class).values());
		CleanthatRepositoryProperties properties = configHelper.loadRepoConfig(new FileSystemResource(pathToConfig));

		codeProviderFormatter.formatCode(properties, codeProvider, false);
	}

	private ICodeProviderWriter makeCodeProvider(Path root) throws IOException {
		ICodeProviderWriter codeProvider;

		// We do not rely on JGit as we do not want to add/commit/push when processing local repository
		// if (root.resolve(".git").toFile().isDirectory()) {
		// LOGGER.info("Processing {} with JGitCodeProvider (as we spot a '.git' directory)");
		// Git jgit = Git.open(root.toFile());
		//
		// codeProvider = JGitCodeProvider.wrap(root, jgit, JGitCodeProvider.getHeadName(jgit.getRepository()));
		// } else {
		LOGGER.info("Processing {} with FileSystemCodeProvider (as we did not spot a '.git' directory)", root);
		codeProvider = new FileSystemCodeProvider(root);
		// }
		return codeProvider;
	}

}
