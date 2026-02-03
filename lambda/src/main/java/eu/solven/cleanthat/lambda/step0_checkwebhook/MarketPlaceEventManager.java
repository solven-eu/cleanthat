/*
 * Copyright 2023-2026 Benoit Lacelle - SOLVEN
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
package eu.solven.cleanthat.lambda.step0_checkwebhook;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import com.github.seratch.jslack.api.webhook.Payload;
import com.github.seratch.jslack.api.webhook.WebhookResponse;
import com.google.common.base.Strings;

import eu.solven.pepper.mappath.MapPathGet;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages marketPlace events for Github
 * 
 * @author Benoit Lacelle
 *
 */
@Slf4j
public class MarketPlaceEventManager {

	protected MarketPlaceEventManager() {
		// hidden
	}

	// https://docs.github.com/en/developers/github-marketplace/using-the-github-marketplace-api-in-your-app/handling-new-purchases-and-free-trials
	// https://docs.github.com/en/developers/github-marketplace/using-the-github-marketplace-api-in-your-app/webhook-events-for-the-github-marketplace-api
	public static void handleMarketplaceEvent(Environment env, Slack slack, Map<String, ?> marketplaceEvent) {
		LOGGER.info("Processing a marketplace event: {}", marketplaceEvent);

		// https://api.slack.com/apps/A04AW22QQ9X/incoming-webhooks
		var whKey = "slack.webhook.marketplace";
		var marketplaceWebhook = env.getProperty(whKey);
		if (Strings.isNullOrEmpty(marketplaceWebhook)) {
			LOGGER.error("We lack a '{}'", whKey);
			return;
		}
		List<LayoutBlock> blocks = new ArrayList<>();

		{
			String json;
			try {
				json = new ObjectMapper().writeValueAsString(marketplaceEvent);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Issue converting event into JSON: " + marketplaceEvent, e);
			}
			// https://api.slack.com/reference/messaging/composition-objects#text
			SectionBlock section = new SectionBlock();
			section.setText(new MarkdownTextObject("```" + json + "```", true));
			blocks.add(section);
		}

		var action = MapPathGet.getRequiredString(marketplaceEvent, "action");
		var effectiveDate = MapPathGet.getRequiredString(marketplaceEvent, "effective_date");
		// Map<String, ?> marketplacePurchase = MapPathGet.getRequiredAs(marketplaceEvent, "marketplace_purchase");
		// String accountLogin = MapPathGet.getRequiredString(marketplacePurchase, "account", "login");

		Payload payload = Payload.builder()
				// The text seems to appear only in the notification, but not in the channel
				.text(action + " (from " + effectiveDate + "):")
				.blocks(blocks)
				.build();
		try {
			WebhookResponse response = slack.send(marketplaceWebhook, payload);

			if (response.getCode() != HttpStatus.SC_OK) {
				LOGGER.warn("Slack response: {}", response);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
