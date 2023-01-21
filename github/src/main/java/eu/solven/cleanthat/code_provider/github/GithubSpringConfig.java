/*
 * Copyright 2023 Solven
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

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.code_provider.github.event.GithubCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.engine.ILanguageLintFixerFactory;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import java.util.List;
import org.kohsuke.github.GitHub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * The {@link Configuration} enabling {@link GitHub}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ CodeCleanerSpringConfig.class })
public class GithubSpringConfig {
	@Bean
	public GithubWebhookHandlerFactory githubWebhookHandler(Environment env, List<ObjectMapper> objectMappers) {
		return new GithubWebhookHandlerFactory(env, objectMappers);
	}

	@Bean
	public GithubCodeCleanerFactory githubCodeCleanerFactory(List<ObjectMapper> objectMappers,
			List<ILanguageLintFixerFactory> factories,
			ICodeProviderFormatter formatterProvider) {
		return new GithubCodeCleanerFactory(objectMappers, factories, formatterProvider);
	}
}
