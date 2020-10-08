package io.cormoran.cleanthat;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.LocalFolderCodeProvider;
import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.lambda.CleanThatLambdaFunction;

@SpringBootApplication
public class RunCleanLocalFolder extends CleanThatLambdaFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanLocalFolder.class);

	ClassPathResource currentRepoSomeFile = new ClassPathResource("/logback.xml");

	public static void main(String[] args) {
		SpringApplication.run(RunCleanLocalFolder.class, args);
	}

	@Bean
	public CodeProviderFormatter codeProviderFormatter() {
		ObjectMapper objectMapper = new ObjectMapper();
		return new CodeProviderFormatter(objectMapper, new JavaFormatter(objectMapper));
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		Path localFolder = currentRepoSomeFile.getFile().toPath();

		// Given the root, we may want to move to a different folder
		String finalRelativePath = "../mitrust-backend";

		// Move up to git repository root folder
		while (!localFolder.resolve(".git").toFile().isDirectory()) {
			localFolder = localFolder.toFile().getParentFile().toPath();
		}
		LOGGER.info("We moved to {}", localFolder);

		LOGGER.info("About to resolve {}", finalRelativePath);
		localFolder = localFolder.resolve(finalRelativePath);

		LOGGER.info("About to process {}", localFolder);

		ApplicationContext appContext = event.getApplicationContext();

		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);

		CleanthatRepositoryProperties properties = appContext.getBean(ObjectMapper.class)
				.readValue(localFolder.resolve("cleanthat.json").toFile(), CleanthatRepositoryProperties.class);
		codeProviderFormatter.formatPR(properties, new LocalFolderCodeProvider(localFolder));
	}

}
