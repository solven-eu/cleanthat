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
package org.kohsuke.github;

import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.kohsuke.github.junit.GitHubWireMockRule;

import com.github.tomakehurst.wiremock.common.NetworkAddressRules;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractGitHubWireMockTest.
 *
 * @author Liam Newman
 */
// https://github.com/hub4j/github-api/blob/main/src/test/java/org/kohsuke/github/AbstractGitHubWireMockTest.java
public abstract class AbstractGitHubWireMockTest {

	private final GitHubBuilder githubBuilder = createGitHubBuilder();

	/** The Constant STUBBED_USER_LOGIN. */
	static final String STUBBED_USER_LOGIN = "placeholder-user";

	/** The Constant STUBBED_USER_PASSWORD. */
	static final String STUBBED_USER_PASSWORD = "placeholder-password";

	/** The use default git hub. */
	protected boolean useDefaultGitHub = true;

	/** The temp git hub repositories. */
	protected final Set<String> tempGitHubRepositories = new HashSet<>();

	/**
	 * {@link GitHub} instance for use during test. Traffic will be part of snapshot when taken.
	 */
	private static GitHub gitHub;

	private static GitHub nonRecordingGitHub;

	/** The base files class path. */
	protected final String baseFilesClassPath = this.getClass().getName().replace('.', '/');

	/** The base record path. */
	protected final String baseRecordPath = "src/test/resources/" + baseFilesClassPath + "/wiremock";

	/** The mock github. */
	@Rule
	public final GitHubWireMockRule mockGitHub;

	/** The templating. */
	protected final TemplatingHelper templating = new TemplatingHelper();

	/**
	 * Instantiates a new abstract git hub wire mock test.
	 */
	public AbstractGitHubWireMockTest() {
		mockGitHub = new GitHubWireMockRule(this.getWireMockOptions());
	}

	public static GitHub getGitHub() {
		return gitHub;
	}

	/**
	 * Gets the wire mock options.
	 *
	 * @return the wire mock options
	 */
	protected WireMockConfiguration getWireMockOptions() {
		return WireMockConfiguration.options()
				.dynamicPort()
				.usingFilesUnderDirectory(baseRecordPath)
				// .networkTrafficListener(new ConsoleNotifyingWiremockNetworkTrafficListener())

				// Unclear why `api.github.com` is considered as not matching the target proxy
				// .limitProxyTargets(NetworkAddressRules.builder().allow("api.github.com").build())
				.limitProxyTargets(NetworkAddressRules.ALLOW_ALL);
	}

	private static GitHubBuilder createGitHubBuilder() {
		// builder = GitHubBuilder.fromProperties(props);
		// builder = GitHubBuilder.fromEnvironment();
		GitHubBuilder builder = fromSystemProperties();
		// builder = GitHubBuilder.fromCredentials();

		return builder.withRateLimitHandler(RateLimitHandler.FAIL);
	}

	// see GitHubBuilder.fromEnvironment()
	protected static GitHubBuilder fromSystemProperties() {
		var properties = new Properties();

		for (Entry<Object, Object> e : System.getProperties().entrySet()) {
			var name = e.getKey().toString().toLowerCase(Locale.ENGLISH);
			if (name.startsWith("github_"))
				name = name.substring("github_".length());

			// Unclear why we keep all properties. Doing like `GitHubBuilder.fromEnvironment()`
			properties.put(name, e.getValue());
		}

		return GitHubBuilder.fromProperties(properties);
	}

	/**
	 * Gets the git hub builder.
	 *
	 * @return the git hub builder
	 */
	protected GitHubBuilder getGitHubBuilder() {
		GitHubBuilder builder = githubBuilder.clone();

		if (!mockGitHub.isUseProxy()) {
			// This sets the user and password to a placeholder for wiremock testing
			// This makes the tests believe they are running with permissions
			// The recorded stubs will behave like they running with permissions
			builder.withPassword(STUBBED_USER_LOGIN, STUBBED_USER_PASSWORD);
		}

		return builder;
	}

	/**
	 * Wire mock setup.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void wireMockSetup() throws Exception {
		GitHubBuilder builder = getGitHubBuilder().withEndpoint(mockGitHub.apiServer().baseUrl());

		// builder = builder.withConnector(GithubAppFactory.createGithubConnector());

		if (useDefaultGitHub) {
			gitHub = builder.build();
		}

		if (mockGitHub.isUseProxy()) {
			nonRecordingGitHub = getGitHubBuilder().withEndpoint("https://api.github.com/").build();
		} else {
			nonRecordingGitHub = null;
		}
	}

	/**
	 * Snapshot not allowed.
	 */
	protected void snapshotNotAllowed() {
		assumeFalse("Test contains hand written mappings. Only valid when not taking a snapshot.",
				mockGitHub.isTakeSnapshot());
	}

	/**
	 * Require proxy.
	 *
	 * @param reason
	 *            the reason
	 */
	protected void requireProxy(String reason) {
		assumeTrue("Test only valid when proxying (-Dtest.github.useProxy to enable): " + reason,
				mockGitHub.isUseProxy());
	}

	/**
	 * Verify authenticated.
	 *
	 * @param instance
	 *            the instance
	 */
	protected static void verifyAuthenticated(GitHub instance) {
		assertThat(
				"GitHub connection believes it is anonymous.  Make sure you set GITHUB_OAUTH or both GITHUB_LOGIN and GITHUB_PASSWORD environment variables",
				instance.isAnonymous(),
				Matchers.is(false));
	}

	/**
	 * Gets the user.
	 *
	 * @return the user
	 */
	protected GHUser getUser() {
		return getUser(getGitHub());
	}

	/**
	 * Gets the user.
	 *
	 * @param gitHub
	 *            the git hub
	 * @return the user
	 */
	protected static GHUser getUser(GitHub gitHub) {
		try {
			return gitHub.getMyself();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Creates a temporary repository that will be deleted at the end of the test. Repository name is based on the
	 * current test method.
	 *
	 * @return a temporary repository
	 * @throws IOException
	 *             if repository could not be created or retrieved.
	 */
	protected GHRepository getTempRepository() throws IOException {
		return getTempRepository("temp-" + this.mockGitHub.getMethodName());
	}

	/**
	 * Creates a temporary repository that will be deleted at the end of the test.
	 *
	 * @param name
	 *            string name of the the repository
	 *
	 * @return a temporary repository
	 * @throws IOException
	 *             if repository could not be created or retrieved.
	 */
	protected GHRepository getTempRepository(String name) throws IOException {
		var fullName = getOrganization() + '/' + name;

		if (mockGitHub.isUseProxy()) {
			cleanupRepository(fullName);

			getCreateBuilder(name).description("A test repository for testing the cleanthat project: " + name)
					.homepage("http://github-api.kohsuke.org/")
					.autoInit(true)
					.wiki(true)
					.downloads(true)
					.issues(true)
					.private_(false)
					.create();
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return getGitHub().getRepository(fullName);
	}

	/**
	 * Cleanup temp repositories.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Before
	@After
	public void cleanupTempRepositories() throws IOException {
		if (mockGitHub.isUseProxy()) {
			for (String fullName : tempGitHubRepositories) {
				cleanupRepository(fullName);
			}
		}
	}

	/**
	 * Cleanup repository.
	 *
	 * @param fullName
	 *            the full name
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected void cleanupRepository(String fullName) throws IOException {
		if (mockGitHub.isUseProxy()) {
			tempGitHubRepositories.add(fullName);
			try {
				GHRepository repository = getNonRecordingGitHub().getRepository(fullName);
				if (repository != null) {
					repository.delete();
				}
			} catch (GHFileNotFoundException e) {
				// Repo already deleted
			}

		}
	}

	/**
	 * {@link GitHub} instance for use before/after test. Traffic will not be part of snapshot when taken. Should only
	 * be used when isUseProxy() or isTakeSnapShot().
	 *
	 * @return a github instance after checking Authentication
	 */
	public static GitHub getNonRecordingGitHub() {
		verifyAuthenticated(nonRecordingGitHub);
		return nonRecordingGitHub;
	}

	private GHCreateRepositoryBuilder getCreateBuilder(String name) throws IOException {
		GitHub github = getNonRecordingGitHub();

		return github.getOrganization(getOrganization()).createRepository(name);
	}

	private String getOrganization() throws IOException {
		return getGitHub().getMyself().getLogin();
	}

	/**
	 * The Class TemplatingHelper.
	 */
	protected static class TemplatingHelper {

		/** The test start date. */
		public Date testStartDate = new Date();

		/**
		 * New response transformer.
		 *
		 * @return the response template transformer
		 */
		public ResponseTemplateTransformer newResponseTransformer() {
			testStartDate = new Date();
			return ResponseTemplateTransformer.builder()
					.global(true)
					.maxCacheEntries(0L)
					// .helper("testStartDate", new Helper<Object>() {
					// private HandlebarsCurrentDateHelper helper = new HandlebarsCurrentDateHelper();
					// @Override
					// public Object apply(final Object context, final Options options) throws IOException {
					// return this.helper.apply(TemplatingHelper.this.testStartDate, options);
					// }
					// })
					.build();
		}
	}

}
