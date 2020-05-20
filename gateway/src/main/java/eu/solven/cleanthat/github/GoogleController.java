package eu.solven.cleanthat.github;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.regions.Regions;

import eu.solven.cleanthat.gateway.lambda.CleanThatLambdaInvoker;

/**
 * Specific to Github.com
 * 
 * @author Benoit Lacelle
 *
 */
@RestController
@RequestMapping("/github")
public class GoogleController {
	final CleanThatLambdaInvoker lambdaInvoker;

	public GoogleController(CleanThatLambdaInvoker lambdaInvoker) {
		this.lambdaInvoker = lambdaInvoker;
	}

	@GetMapping("on_installation")
	public Map<String, ?> onInstallation() {
		return Map.of("message", "Happy CleanThat new user!");
	}

	@PostMapping("webhook")
	public Map<String, ?> onWebhook(@RequestBody Map<String, ?> payload) {
		// https://stackoverflow.com/questions/43452540/where-are-heroku-apps-hosted-exactly/45229837
		lambdaInvoker.runWithPayload(Regions.US_EAST_1.getName(), "upperCase", payload);

		return Map.of("message", "Happy CleanThat new user!");
	}
}
