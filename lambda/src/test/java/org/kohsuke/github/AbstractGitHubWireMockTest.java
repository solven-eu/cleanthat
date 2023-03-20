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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.kohsuke.github.junit.GitHubWireMockRule;

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

	/** The Constant GITHUB_API_TEST_ORG. */
	static final String GITHUB_API_TEST_ORG = "hub4j-test-org";

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
	protected static GitHub gitHub;

	private GitHub nonRecordingGitHub;

	/** The base files class path. */
	protected final String baseFilesClassPath = this.getClass().getName().replace('.', '/');

	/** The base record path. */
	protected final String baseRecordPath = "src/test/resources/" + baseFilesClassPath + "/wiremock";

	/** The mock git hub. */
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
		return WireMockConfiguration.options().dynamicPort().usingFilesUnderDirectory(baseRecordPath);
	}

	private static GitHubBuilder createGitHubBuilder() {

		GitHubBuilder builder = new GitHubBuilder();

		try {
			var f = new File(System.getProperty("user.home"), ".github.kohsuke2");
			if (f.exists()) {
				var props = new Properties();
				FileInputStream in = null;
				try {
					in = new FileInputStream(f);
					props.load(in);
				} finally {
					IOUtils.closeQuietly(in);
				}
				// use the non-standard credential preferentially, so that developers of this library do not have
				// to clutter their event stream.
				builder = GitHubBuilder.fromProperties(props);
			} else {

				builder = GitHubBuilder.fromEnvironment();

				// builder = GitHubBuilder.fromCredentials();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return builder.withRateLimitHandler(RateLimitHandler.FAIL);
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
	protected void verifyAuthenticated(GitHub instance) {
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

			getCreateBuilder(name).description("A test repository for testing the github-api project: " + name)
					.homepage("http://github-api.kohsuke.org/")
					.autoInit(true)
					.wiki(true)
					.downloads(true)
					.issues(true)
					.private_(false)
					.create();
			try {
				Thread.sleep(3_000);
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
	public GitHub getNonRecordingGitHub() {
		verifyAuthenticated(nonRecordingGitHub);
		return nonRecordingGitHub;
	}

	private GHCreateRepositoryBuilder getCreateBuilder(String name) throws IOException {
		GitHub github = getNonRecordingGitHub();

		return github.getOrganization(getOrganization()).createRepository(name);
	}

	private String getOrganization() throws IOException {
		if (mockGitHub.isTestWithOrg()) {
			return GITHUB_API_TEST_ORG;
		} else {
			return getGitHub().getMyself().getLogin();
		}
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
