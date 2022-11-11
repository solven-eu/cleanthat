package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.event.GithubNoApiWebhookHandler;
import eu.solven.cleanthat.code_provider.github.event.pojo.GithubWebhookEvent;
import eu.solven.cleanthat.codeprovider.git.GitWebhookRelevancyResult;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.IGitRefsConstants;

public class TestGithubWebhookHandler {

	final GithubNoApiWebhookHandler handler =
			new GithubNoApiWebhookHandler(Arrays.asList(ConfigHelpers.makeJsonObjectMapper()));

	final ObjectMapper objectMapper = new ObjectMapper();

	// @Ignore("Issue with GitHub mocking. Needs to add an abstract layer on top of Github")
	@Test
	public void processTestOpenPR() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> input = objectMapper
				.readValue(new ClassPathResource("/github/webhook.pull_request.json").getInputStream(), Map.class);
		// Mockito.when(repo.getPullRequest(Mockito.anyInt())).thenReturn(pr);
		handler.filterWebhookEventRelevant(new GithubWebhookEvent(input));
		// Mockito.verify(repo).getPullRequest(2);
		// Mockito.verify(prCleaner)
		// .formatPR(Mockito.any(Optional.class), Mockito.eq(new AtomicInteger()), Mockito.eq(pr));
	}

	// @Ignore("Issue with GitHub mocking. Needs to add an abstract layer on top of Github")
	@Test
	public void processTestPush() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> input =
				objectMapper.readValue(new ClassPathResource("/github/webhook.push.json").getInputStream(), Map.class);
		handler.filterWebhookEventRelevant(new GithubWebhookEvent(input));
	}

	@Test
	public void testParseEvent_openPr() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
				.readValue(new ClassPathResource("/github/webhook/pr_open-open_event.json").getInputStream(),
						Map.class);
		GitWebhookRelevancyResult result = handler.filterWebhookEventRelevant(new GithubWebhookEvent(body));

		Assertions.assertThat(result.isReviewRequestOpen()).isTrue();
		Assertions.assertThat(result.isPushRef()).isFalse();

		Assertions.assertThat(result.optBaseRef().get().getRef()).isEqualTo("refs/heads/master");
		Assertions.assertThat(result.optBaseRef().get().getRepoFullName()).isEqualTo("solven-eu/cleanthat");
		Assertions.assertThat(result.optBaseRef().get().getSha()).isEqualTo("957d4697599b1cea7b81f60dddbe3edfb236f2af");
	}

	@Test
	public void testParseEvent_push000AsBase() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
				.readValue(new ClassPathResource("/github/push-000AsBase.json").getInputStream(), Map.class);
		GitWebhookRelevancyResult result = handler.filterWebhookEventRelevant(new GithubWebhookEvent(body));

		Assertions.assertThat(result.isReviewRequestOpen()).isFalse();
		Assertions.assertThat(result.isPushRef()).isTrue();

		Assertions.assertThat(result.optBaseRef().get().getRepoFullName()).isEqualTo("solven-eu/mitrust-datasharing");
		Assertions.assertThat(result.optBaseRef().get().getRef())
				.isEqualTo("refs/heads/renovate/major-vue-cli-monorepo");
		Assertions.assertThat(result.optBaseRef().get().getSha())
				.isEqualTo(IGitRefsConstants.SHA1_CLEANTHAT_UP_TO_REF_ROOT);

		Assertions.assertThat(result.optPushedRefOrRrHead().get().getRepoFullName())
				.isEqualTo("solven-eu/mitrust-datasharing");
		Assertions.assertThat(result.optPushedRefOrRrHead().get().getRef())
				.isEqualTo("refs/heads/renovate/major-vue-cli-monorepo");
	}

	@Test
	public void testParseEvent_forcePush() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> body = ConfigHelpers.makeJsonObjectMapper()
				.readValue(new ClassPathResource("/github/push_force.json").getInputStream(), Map.class);
		GitWebhookRelevancyResult result = handler.filterWebhookEventRelevant(new GithubWebhookEvent(body));

		Assertions.assertThat(result.isReviewRequestOpen()).isFalse();
		Assertions.assertThat(result.isPushRef()).isTrue();

		Assertions.assertThat(result.optBaseRef().get().getRepoFullName()).isEqualTo("solven-eu/cleanthat");
		Assertions.assertThat(result.optBaseRef().get().getRef()).isEqualTo("refs/heads/renovate/spotbugs");
		Assertions.assertThat(result.optBaseRef().get().getSha())
				.isEqualTo(IGitRefsConstants.SHA1_CLEANTHAT_UP_TO_REF_ROOT);

		Assertions.assertThat(result.optPushedRefOrRrHead().get().getRepoFullName()).isEqualTo("solven-eu/cleanthat");
		Assertions.assertThat(result.optPushedRefOrRrHead().get().getRef()).isEqualTo("refs/heads/renovate/spotbugs");
	}
}
