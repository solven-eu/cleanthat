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

import org.junit.runner.RunWith;
import org.kohsuke.github.AbstractGitHubWireMockTest;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import eu.solven.cleanthat.code_provider.github.event.GithubAppFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubAppFactory;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step2_executeclean.ExecuteCleaningWebhooksLambdaFunction;

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

	public static final String ENV_GITHUB_DO_RECORD = "github.do_record";

	@Autowired
	Environment env;

	@Autowired
	AWebhooksLambdaFunction lambdaFunction;

	@Configuration
	public static class CleanthatWiremockSpringConfigContext {
		@Primary
		@Bean
		public IGithubAppFactory wiremockGhFactory(Environment env) {
			if (!env.getProperty(ENV_GITHUB_DO_RECORD, Boolean.class, false)) {
				return null;
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
	}

}
