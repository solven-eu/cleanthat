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
package eu.solven.cleanthat.code_provider.github;

import java.util.List;

import org.kohsuke.github.GitHub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.GithubAppFactory;
import eu.solven.cleanthat.code_provider.github.event.GithubCheckRunManager;
import eu.solven.cleanthat.code_provider.github.event.GithubCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubAppFactory;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.ICleanthatConfigInitializer;
import eu.solven.cleanthat.config.IGitService;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;

/**
 * The {@link Configuration} enabling {@link GitHub} as an {@link ICodeProvider}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ CodeCleanerSpringConfig.class })
public class GithubSpringConfig {
	@Bean
	public IGithubAppFactory githubAppFactory(Environment env) {
		return new GithubAppFactory(env);
	}

	@Bean
	public GithubCheckRunManager githubCheckRunManager(IGitService gitService) {
		return new GithubCheckRunManager(gitService);
	}

	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(IGithubAppFactory githubAppFactory,
			List<ObjectMapper> objectMappers,
			GithubCheckRunManager githubCheckRunManager) {
		return new GithubWebhookHandlerFactory(githubAppFactory, objectMappers, githubCheckRunManager);
	}

	@Bean
	public GithubCodeCleanerFactory githubCodeCleanerFactory(List<ObjectMapper> objectMappers,
			ICleanthatConfigInitializer configInitializer,
			ICodeProviderFormatter formatterProvider,
			GithubCheckRunManager githubCheckRunManager) {
		return new GithubCodeCleanerFactory(objectMappers, configInitializer, formatterProvider, githubCheckRunManager);
	}

}
