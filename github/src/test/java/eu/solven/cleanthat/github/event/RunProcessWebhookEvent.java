package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.GithubNoApiWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.config.ConfigHelpers;

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
