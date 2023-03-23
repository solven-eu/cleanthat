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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.junit.GitHubWireMockRule;
import org.kohsuke.github.junit.WireMockMultiServerRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.aws.dynamodb.it.AProcessLocallyDynamoDbEvent_ExecuteClean;
import eu.solven.cleanthat.aws.dynamodb.it.EventFromDynamoDbITHelper;
import eu.solven.cleanthat.code_provider.github.event.GithubAppFactory;
import eu.solven.cleanthat.code_provider.github.event.IGithubAppFactory;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;

/**
 * This is an example of recorded event.
 * 
 * @author Benoit Lacelle
 *
 */
@Ignore("Still WIP")
@RunWith(SpringRunner.class)
public class TestExecuteClean_FirstRecord extends AProcessLocallyDynamoDbEvent_ExecuteClean {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestExecuteClean_FirstRecord.class);

	// Please make a first run with this set to true, then keep it to false;
	public static boolean DO_RECORD = true;

	// https://stackoverflow.com/questions/33855874/testpropertysource-with-dynamic-properties
	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add(ENV_GITHUB_DO_RECORD, () -> {
			return Boolean.toString(DO_RECORD);
		});
	}

	static {
		if (DO_RECORD) {
			LOGGER.info("Recording is enabled: We will register a JWT for the recording session");
			// This will enable recording of GithubAPI response, enabling replaying
			System.setProperty(GitHubWireMockRule.KEY_TAKE_SNAPSHOT, "true");

			Environment env = new StandardEnvironment();
			GithubAppFactory githubAppFactory = new GithubAppFactory(env);

			try {
				githubAppFactory.makeAppGithub().getApp();
				// see org.kohsuke.github.GitHubBuilder.fromEnvironment()
				System.setProperty("GITHUB_JWT", githubAppFactory.makeJWT());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (JOSEException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Autowired
	AWebhooksLambdaFunction lambdaFunction;

	@Autowired
	IGithubAppFactory wireMockAppFactory;

	@Test
	public void testRecorded() throws IOException, JOSEException {
		// This is logged by: e.s.c.lambda.AWebhooksLambdaFunction|parseDynamoDbEvent
		// You can search logs for this key, in order to process given event locally
		var key = "random-76596173-8e73-4e17-930b-bff57f342078";

		Map<String, ?> dynamoDbPureJson;

		Path path = GitHubWireMockRule.fileToServerRecords(WireMockMultiServerRule.SERVER_API, mockGitHub.apiServer());
		ObjectMapper objectMapper = new ObjectMapper();
		if (DO_RECORD) {
			dynamoDbPureJson = EventFromDynamoDbITHelper.loadEvent("cleanthat_accepted_events", key);

			objectMapper.writeValue(path.toFile(), dynamoDbPureJson);
		} else {
			dynamoDbPureJson = objectMapper.readValue(path.toFile(), Map.class);
		}

		Map<String, ?> output = lambdaFunction.ingressRawWebhook().apply(dynamoDbPureJson);

		Assertions.assertThat(output).hasSize(1);
	}
}
