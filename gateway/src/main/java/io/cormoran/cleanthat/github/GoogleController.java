package io.cormoran.cleanthat.github;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/github")
public class GoogleController {
	@GetMapping("on_installation")
	public Map<String, ?> onInstallation() {
		return Map.of("message", "Happy CleanThat new user!");
	}

	@PostMapping("webhook")
	public Map<String, ?> onWebhook(@RequestBody Map<String, ?> payload) {
		return Map.of("message", "Happy CleanThat new user!");
	}
}
