package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.github.event.pojo.GithubWebhookEvent;

public class TestGithubWebhookHandler {

	// Relevant to rely on GitHub.offline()?
	final GitHub github = Mockito.mock(GitHub.class);

	final GitHub installGithub = Mockito.mock(GitHub.class);

	final GHRepository repo = Mockito.mock(GHRepository.class);

	final GHPullRequest pr = Mockito.mock(GHPullRequest.class);

	final IGithubRefCleaner prCleaner = Mockito.mock(IGithubRefCleaner.class);

	final GithubWebhookHandler handler =
			new GithubWebhookHandler(github, Arrays.asList(ConfigHelpers.makeJsonObjectMapper())) {

				@Override
				protected GitHub makeInstallationGithub(String token) throws IOException {
					return installGithub;
				}
			};

	final ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void initMocks() throws IOException {
		GHApp ghApp = Mockito.mock(GHApp.class);
		Mockito.when(github.getApp()).thenReturn(ghApp);
		GHAppInstallation appInstall = Mockito.mock(GHAppInstallation.class);
		Mockito.when(ghApp.getInstallationById(Mockito.anyLong())).thenReturn(appInstall);
		GHAppCreateTokenBuilder tokenBuilder = Mockito.mock(GHAppCreateTokenBuilder.class);
		Mockito.when(appInstall.createToken(Mockito.anyMap())).thenReturn(tokenBuilder);
		GHAppInstallationToken installToken = Mockito.mock(GHAppInstallationToken.class);
		Mockito.when(tokenBuilder.create()).thenReturn(installToken);
		Mockito.when(installGithub.getRepositoryById(Mockito.anyString())).thenReturn(repo);

		// Mockito.when(appInstall.getGith)
		Mockito.when(installGithub.getRateLimit()).thenReturn(Mockito.mock(GHRateLimit.class));
	}

	@Ignore("Issue with GitHub mocking")
	@Test
	public void processTestOpenPR() throws JsonParseException, JsonMappingException, IOException {
		GHUser ghUser = Mockito.mock(GHUser.class);
		Mockito.doReturn(123456789L).when(ghUser).getId();
		Mockito.when(pr.getUser()).thenReturn(ghUser);

		Map<String, ?> input = objectMapper
				.readValue(new ClassPathResource("/github/webhook.pull_request.json").getInputStream(), Map.class);
		Mockito.when(repo.getPullRequest(Mockito.anyInt())).thenReturn(pr);
		handler.isWebhookEventRelevant(new GithubWebhookEvent(input));
		// Mockito.verify(repo).getPullRequest(2);
		// Mockito.verify(prCleaner)
		// .formatPR(Mockito.any(Optional.class), Mockito.eq(new AtomicInteger()), Mockito.eq(pr));
	}

	@Ignore("Issue with GitHub mocking. Needs to add an abstract layer on top of Github")
	@Test
	public void processTestPush() throws JsonParseException, JsonMappingException, IOException {
		Map<String, ?> input =
				objectMapper.readValue(new ClassPathResource("/github/webhook.push.json").getInputStream(), Map.class);
		handler.isWebhookEventRelevant(new GithubWebhookEvent(input));
	}
}
