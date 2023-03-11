/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.local.FileSystemGitCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.CodeWritingMetadata;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.config.ICleanthatConfigInitializer;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.jgit.JGitCodeProvider;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;

/**
 * This enables easy cleaning of any given folder. Given folder is supposedly the root of a repository.
 * 
 * You may for example checkout 'git@github.com:spring-projects/spring-boot.git' in 'cleanthat-ITs'.
 * 
 * 
 * @author Benoit Lacelle
 *
 */
public class ITCleanLocalRepository extends ACleanThatXxxApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITCleanLocalRepository.class);

	public static void main(String[] args) {
		SpringApplication springApp = new SpringApplication(ITCleanLocalRepository.class);

		springApp.setWebApplicationType(WebApplicationType.NONE);

		springApp.run(args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		// One can adjust this to any local folder
		var repoFolder = Paths.get(System.getProperty("user.home"), "cleanthat-ITs", "spring-boot");

		LOGGER.info("About to process {}", repoFolder);

		var codeProvider = makeCodeProvider(repoFolder);

		var appContext = event.getApplicationContext();
		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		Optional<File> optConfig = CodeProviderHelpers.pathToConfig(repoFolder);
		if (optConfig.isEmpty()) {
			LOGGER.info("Generate an initial configuration");
			ICleanthatConfigInitializer initializer = appContext.getBean(ICleanthatConfigInitializer.class);
			var result = initializer.prepareFile(codeProvider, false, "ITCleanLocalRepository");

			codeProvider.persistChanges(result.getPathToContents(), CodeWritingMetadata.empty());

			optConfig = CodeProviderHelpers.pathToConfig(repoFolder);
		}
		var pathToConfig = optConfig.get();

		var configHelper = new ConfigHelpers(appContext.getBeansOfType(ObjectMapper.class).values());
		var properties = configHelper.loadRepoConfig(new FileSystemResource(pathToConfig));

		var dryRun = false;
		codeProviderFormatter.formatCode(properties, codeProvider, dryRun);
	}

	private ICodeProviderWriter makeCodeProvider(Path root) throws IOException {
		ICodeProviderWriter codeProvider;

		if (root.resolve(".git").toFile().isDirectory()) {
			LOGGER.info("Processing {} with JGitCodeProvider (as we spot a '.git' directory)", root);
			Git jgit = Git.open(root.toFile());

			// We can rely on JGit but we do not want to add/commit/push when processing local repository
			// As the point is to look at the produced output
			var commitPush = false;
			codeProvider =
					JGitCodeProvider.wrap(root, jgit, JGitCodeProvider.getHeadName(jgit.getRepository()), commitPush);
		} else {
			LOGGER.info("Processing {} with FileSystemCodeProvider (as we did not spot a '.git' directory)", root);
			codeProvider = new FileSystemGitCodeProvider(root);
		}
		return codeProvider;
	}

}
