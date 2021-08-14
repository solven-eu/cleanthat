package eu.solven.cleanthat.github.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;

public class RunCleanLocalFolder extends ACleanThatXxxApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanLocalFolder.class);

	final Resource currentRepoSomeFile = new ClassPathResource("/logback.xml");

	public static void main(String[] args) {
		SpringApplication springApp = new SpringApplication(RunCleanLocalFolder.class);

		springApp.setWebApplicationType(WebApplicationType.NONE);

		springApp.run(args);
	}

	// @Bean
	// public CodeProviderFormatter codeProviderFormatter() {
	// ObjectMapper objectMapper = new ObjectMapper();
	// return new CodeProviderFormatter(objectMapper, new JavaFormatter(objectMapper));
	// }

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
		finalRelativePath = "../mitrust-datasharing";

		LOGGER.info("About to resolve {}", finalRelativePath);
		localFolder = localFolder.resolve(finalRelativePath).normalize();
		LOGGER.info("About to process {}", localFolder);
		ApplicationContext appContext = event.getApplicationContext();
		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		File pathToConfig = CodeProviderHelpers.pathToConfig(localFolder);
		CleanthatRepositoryProperties properties =
				appContext.getBean(ObjectMapper.class).readValue(pathToConfig, CleanthatRepositoryProperties.class);
		codeProviderFormatter.formatCode(properties, new FileSystemCodeProvider(localFolder), false);
	}
}
