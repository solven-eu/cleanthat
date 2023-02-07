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
package eu.solven.cleanthat.github.event;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.solven.cleanthat.code_provider.github.event.GithubNoApiWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.config.ConfigHelpers;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

// https://github.com/organizations/solven-eu/settings/apps/cleanthat/advanced
public class RunProcessWebhookEvent {

	final GithubNoApiWebhookHandler handler =
			new GithubNoApiWebhookHandler(Arrays.asList(ConfigHelpers.makeJsonObjectMapper()));

	final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void processTestPush() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> input = objectMapper
				.readValue(new ClassPathResource("/github/webhook/oneshot.json").getInputStream(), Map.class);

		GitWebhookRelevancyResult result = handler.filterWebhookEventRelevant(new GithubWebhookEvent(input));
	}
}
