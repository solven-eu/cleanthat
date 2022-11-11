package eu.solven.cleanthat.lambda.step0_checkwebhook;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import com.github.seratch.jslack.api.webhook.Payload;
import com.github.seratch.jslack.api.webhook.WebhookResponse;
import com.google.common.base.Strings;

import eu.solven.pepper.collection.PepperMapHelper;

public class MarketPlaceEventManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketPlaceEventManager.class);

	// https://docs.github.com/en/developers/github-marketplace/using-the-github-marketplace-api-in-your-app/handling-new-purchases-and-free-trials
	// https://docs.github.com/en/developers/github-marketplace/using-the-github-marketplace-api-in-your-app/webhook-events-for-the-github-marketplace-api
	public static void handleMarketplaceEvent(Environment env, Slack slack, Map<String, ?> marketplaceEvent) {
		LOGGER.info("Processing a marketplace event: {}", marketplaceEvent);

		String action = PepperMapHelper.getRequiredString(marketplaceEvent, "action");
		String effectiveDate = PepperMapHelper.getRequiredString(marketplaceEvent, "effective_date");
		// Map<String, ?> marketplacePurchase = PepperMapHelper.getRequiredAs(marketplaceEvent, "marketplace_purchase");
		// String accountLogin = PepperMapHelper.getRequiredString(marketplacePurchase, "account", "login");

		List<LayoutBlock> blocks = new ArrayList<>();

		{
			// https://api.slack.com/reference/messaging/composition-objects#text
			SectionBlock section = new SectionBlock();
			String json;
			try {
				json = new ObjectMapper().writeValueAsString(marketplaceEvent);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Issue converting event into JSON: " + marketplaceEvent, e);
			}
			section.setText(new MarkdownTextObject("```" + json + "```", true));
			blocks.add(section);
		}

		Payload payload = Payload.builder()
				// The text seems to appear only in the notification, but not in the channel
				.text(action + " (from " + effectiveDate + "):")
				.blocks(blocks)
				.build();

		// https://api.slack.com/apps/A04AW22QQ9X/incoming-webhooks
		String whKey = "slack.webhook.marketplace";
		String marketplaceWebhook = env.getProperty(whKey);
		if (Strings.isNullOrEmpty(marketplaceWebhook)) {
			LOGGER.error("We lack a '{}'", whKey);
			return;
		}
		try {
			WebhookResponse response = slack.send(marketplaceWebhook, payload);

			if (response.getCode() != HttpStatus.OK.value()) {
				LOGGER.warn("Slack response: {}", response);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
