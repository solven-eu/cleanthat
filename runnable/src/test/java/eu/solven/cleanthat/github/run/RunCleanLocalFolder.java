package eu.solven.cleanthat.github.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.jgit.LocalFolderCodeProvider;
import eu.solven.cleanthat.lambda.ACleanThatXxxFunction;

public class RunCleanLocalFolder extends ACleanThatXxxFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanLocalFolder.class);

	final ClassPathResource currentRepoSomeFile = new ClassPathResource("/logback.xml");

	public static void main(String[] args) {
		new SpringApplication(RunCleanLocalFolder.class).run(args);
	}

	@Bean
	public CodeProviderFormatter codeProviderFormatter() {
		ObjectMapper objectMapper = new ObjectMapper();
		return new CodeProviderFormatter(objectMapper, new JavaFormatter(objectMapper));
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		Path localFolder = currentRepoSomeFile.getFile().toPath();
		// Move up to git repository root folder
		while (!localFolder.resolve(".git").toFile().isDirectory()) {
			localFolder = localFolder.toFile().getParentFile().toPath();
		}
		LOGGER.info("We moved to {}", localFolder);
		// Given the root, we may want to move to a different folder
		String finalRelativePath = ".";
		// We'd better processing a sibling folder/repository
		// finalRelativePath = "../mitrust-backend";
		LOGGER.info("About to resolve {}", finalRelativePath);
		localFolder = localFolder.resolve(finalRelativePath).normalize();
		LOGGER.info("About to process {}", localFolder);
		ApplicationContext appContext = event.getApplicationContext();
		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		File pathToConfig = localFolder.resolve(GithubPullRequestCleaner.FILENAME_CLEANTHAT_JSON).toFile();
		// We prefer to test with a different configuration
		String overridePackage = "/eu.solven/mitrust-datasharing/";
		pathToConfig =
				new ClassPathResource("/overrides" + overridePackage + GithubPullRequestCleaner.FILENAME_CLEANTHAT_JSON)
						.getFile();
		CleanthatRepositoryProperties properties =
				appContext.getBean(ObjectMapper.class).readValue(pathToConfig, CleanthatRepositoryProperties.class);
		codeProviderFormatter.formatCode(properties, new LocalFolderCodeProvider(localFolder));
	}
}
