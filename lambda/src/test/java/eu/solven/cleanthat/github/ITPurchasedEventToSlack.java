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
package eu.solven.cleanthat.github;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.seratch.jslack.Slack;

import eu.solven.cleanthat.lambda.step0_checkwebhook.MarketPlaceEventManager;
import eu.solven.pepper.resource.PepperResourceHelper;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { Slack.class })
@TestPropertySource(properties = "slack.webhook.marketplace=https://hooks.slack.com/services/A/B/C")
public class ITPurchasedEventToSlack {
	@Autowired
	Environment env;

	@Autowired
	Slack slack;

	@Test
	public void testPurchasedEvent() throws JsonMappingException, JsonProcessingException {
		MarketPlaceEventManager.handleMarketplaceEvent(env,
				slack,
				new ObjectMapper().readValue(PepperResourceHelper.loadAsString("/examples/github/purchased.json"),
						Map.class));
	}
}
