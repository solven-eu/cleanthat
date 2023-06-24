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
package eu.solven.cleanthat.aws.dynamodb.it;

import java.io.IOException;
import java.util.Set;

import org.junit.runner.RunWith;
import org.kohsuke.github.AbstractGitHubWireMockTest;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.code_provider.github.event.GithubAppFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubAppFactory;
import eu.solven.cleanthat.config.pojo.CleanthatStepProperties;
import eu.solven.cleanthat.engine.IEngineFormatterFactory;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;
import eu.solven.cleanthat.formatter.CleanthatSession;
import eu.solven.cleanthat.formatter.ILintFixer;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step2_executeclean.ExecuteCleaningWebhooksLambdaFunction;
import eu.solven.cleanthat.language.IEngineProperties;

/**
 * This enables re-processing an event locally. Very useful to reproduce an issue, or test a know workload over a
 * different codebase
 * 
 * @author Benoit Lacelle
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ExecuteCleaningWebhooksLambdaFunction.class,
		AProcessLocallyDynamoDbEvent_ExecuteClean.CleanthatWiremockSpringConfigContext.class })
public abstract class AProcessLocallyDynamoDbEvent_ExecuteClean extends AbstractGitHubWireMockTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AProcessLocallyDynamoDbEvent_ExecuteClean.class);

	public static final String ENV_GITHUB_DO_RECORD = "github.do_record";
	public static final String ENV_GITHUB_DO_REPLAY = "github.do_replay";

	@Autowired
	Environment env;

	@Autowired
	AWebhooksLambdaFunction lambdaFunction;

	@Configuration
	public static class CleanthatWiremockSpringConfigContext {
		@Primary
		@Bean
		public IGithubAppFactory wiremockGhFactory(Environment env) {
			boolean doRecord = env.acceptsProfiles(Profiles.of(ENV_GITHUB_DO_RECORD));
			boolean doReplay = env.acceptsProfiles(Profiles.of(ENV_GITHUB_DO_REPLAY));

			if (doRecord && doReplay) {
				throw new IllegalStateException("Can not both record and replay");
			} else if (!doRecord && !doReplay) {
				// Should rely on default GithubSpringConfig.githubAppFactory(Environment)
				// But returning a null bean lead to bean appearing as missing
				return new GithubSpringConfig().githubAppFactory(env);
			} else if (doRecord) {
				LOGGER.info("Recording Github API");
			} else if (doReplay) {
				LOGGER.info("Replaying Github API");
			}

			GithubAppFactory wireMockAppFactory = new GithubAppFactory(env) {
				@Override
				public GitHub makeAppGithub() {
					GitHub gitHub = AbstractGitHubWireMockTest.getGitHub();

					if (gitHub == null) {
						throw new IllegalStateException("The mocked GitHub is not yet ready");
					}

					return gitHub;
				}
			};

			return wireMockAppFactory;
		}

		// When replaying, we want to skip the potentially slow actual linting
		@Profile(ENV_GITHUB_DO_REPLAY)
		@Primary
		@Bean
		public IEngineFormatterFactory mockedEngineFormatterFactory(Environment env) throws IOException {
			IEngineFormatterFactory factory = Mockito.mock(IEngineFormatterFactory.class);

			IEngineLintFixerFactory lintFixerFactory = Mockito.mock(IEngineLintFixerFactory.class);

			ILintFixer lintFixer = Mockito.mock(ILintFixer.class);
			Mockito.when(lintFixer.doFormat(Mockito.anyString()))
					.thenReturn("FakeLinted.AProcessLocallyDynamoDbEvent_ExecuteClean");

			Mockito.when(lintFixerFactory.makeLintFixer(Mockito.any(CleanthatSession.class),
					Mockito.any(IEngineProperties.class),
					Mockito.any(CleanthatStepProperties.class))).thenReturn(lintFixer);

			Mockito.when(factory.getDefaultIncludes(Mockito.anyString())).thenReturn(Set.of("glob:**/*.java"));

			Mockito.when(factory.makeLanguageFormatter(Mockito.any(IEngineProperties.class)))
					.thenReturn(lintFixerFactory);

			return factory;
		}
	}

}
