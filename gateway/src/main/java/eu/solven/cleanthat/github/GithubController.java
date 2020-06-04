package eu.solven.cleanthat.github;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.gateway.lambda.CleanThatLambdaInvoker;

/**
 * Specific to Github.com
 * 
 * @author Benoit Lacelle
 *
 */
@RestController
@RequestMapping("/github")
public class GithubController {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubController.class);

	final CleanThatLambdaInvoker lambdaInvoker;
	final ObjectMapper objectMapper;

	public GithubController(CleanThatLambdaInvoker lambdaInvoker, ObjectMapper objectMapper) {
		this.lambdaInvoker = lambdaInvoker;
		this.objectMapper = objectMapper;
	}

	@GetMapping("on_installation")
	public Map<String, ?> onInstallation() {
		return Map.of("message", "Happy CleanThat new user!");
	}

	@PostMapping("webhook")
	public Map<String, ?> onWebhook(@RequestHeader(value = "X-Hub-Signature", required = false) String githubSignature,
			@RequestBody String body) {
		// Used for the sake of checking what inputs actually looks like
		LOGGER.info("TMP Body: {}", body);

		Map<?, ?> payload;
		try {
			payload = objectMapper.readValue(body, Map.class);
		} catch (JsonProcessingException e) {
			LOGGER.warn("Issue with body", e);
			return Map.of("status", "ARG");
		}

		verifySignature(githubSignature);

		processPayload(payload);

		return Map.of("status", "accepted");
	}

	protected void processPayload(Map<?, ?> payload) {
		// https://stackoverflow.com/questions/43452540/where-are-heroku-apps-hosted-exactly/45229837
		lambdaInvoker.runWithPayload(Regions.US_EAST_1.getName(), "upperCase", payload);
	}

	// https://developer.github.com/webhooks/securing/#validating-payloads-from-github
	private void verifySignature(String githubSignature) {
		LOGGER.warn("TODO Implement Github signature verification: {}", githubSignature);
	}
}
