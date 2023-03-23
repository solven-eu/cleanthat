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
package eu.solven.cleanthat.aws.dynamodb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

// https://github.com/wiremock/wiremock/issues/1961
@SpringBootTest(classes = WiremockTest.Application.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WiremockTest {

	@RegisterExtension
	static WireMockExtension wiremock = WireMockExtension.newInstance()
			.proxyMode(true)
			.options(wireMockConfig().dynamicPort().notifier(new ConsoleNotifier(true)))
			.build();

	@SpringBootApplication
	static class Application {

		@RestController
		@RequestMapping("/api")
		static class Controller {

			@GetMapping
			public Map<String, String> get(@RequestHeader Map<String, String> headers) {
				return headers;
			}
		}
	}

	@Test
	void test() {
		wiremock.stubFor(get("/api").withHost(equalTo("some-external-service.com"))
				.willReturn(aResponse().proxiedFrom("http://localhost:8080")
						.withAdditionalRequestHeader("Authorization", "Bearer az.we.rt-ty-yu")
						.withAdditionalRequestHeader("Accept", "application/vnd.github.machine-man-preview+json")));
		RestTemplate restTemplate = new RestTemplate();
		Map<String, String> headers = restTemplate.getForObject("http://some-external-service.com/api", Map.class);
		assertThat(headers).containsEntry("accept", "application/vnd.github.machine-man-preview+json")
				.containsEntry("authorization", "Bearer az.we.rt-ty-yu");
	}
}