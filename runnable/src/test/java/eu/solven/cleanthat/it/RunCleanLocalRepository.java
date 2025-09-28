/*
 * Copyright 2023-2025 Benoit Lacelle - SOLVEN
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.code_provider.local.FileSystemGitCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.lambda.ACleanThatXxxApplication;
import lombok.extern.slf4j.Slf4j;

/**
 * This enables easy cleaning of any given folder. Given folder is supposedly the root of a repository.
 * 
 * You may for example checkout 'git@github.com:spring-projects/spring-boot.git' in 'cleanthat-ITs'.
 * 
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class RunCleanLocalRepository extends ACleanThatXxxApplication {

	public static void main(String[] args) {
		SpringApplication springApp = new SpringApplication(RunCleanLocalRepository.class);

		springApp.setWebApplicationType(WebApplicationType.NONE);

		springApp.run(args);
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		// One can adjust this to any local folder
		var repoFolder = Paths.get(System.getProperty("user.home"), "workspace3", "spring-boot");

		LOGGER.info("About to process {}", repoFolder);

		var codeProvider = makeCodeProvider(repoFolder);

		var appContext = event.getApplicationContext();
		CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		var pathToConfig = CodeProviderHelpers.pathToConfig(repoFolder).get();

		var configHelper = new ConfigHelpers(appContext.getBeansOfType(ObjectMapper.class).values());
		var properties = configHelper.loadRepoConfig(new FileSystemResource(pathToConfig));

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
		codeProvider = new FileSystemGitCodeProvider(root);
		// }
		return codeProvider;
	}

}
