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
