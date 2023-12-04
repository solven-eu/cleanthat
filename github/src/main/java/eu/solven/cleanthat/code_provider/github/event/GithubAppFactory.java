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
package eu.solven.cleanthat.code_provider.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.Map;
import java.util.function.Supplier;

import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.extras.authorization.JWTTokenProvider;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.google.common.base.Ascii;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.impl.RSAKeyUtils;
import com.nimbusds.jose.jwk.RSAKey;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.solven.cleanthat.code_provider.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.utils.ResultOrError;
import okhttp3.OkHttpClient;

/**
 * Default implementation for {@link IGithubAppFactory}
 * 
 * @author Benoit Lacelle
 *
 */
// https://github.com/spotbugs/spotbugs/issues/2695
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class GithubAppFactory implements IGithubAppFactory {
	private static final int PUBLIC_CHARS_IN_TOKEN = 10;

	private static final Logger LOGGER = LoggerFactory.getLogger(GithubAppFactory.class);

	public static final String ENV_GITHUB_APP_PRIVATE_JWK = "github.app.private-jwk";

	// https://github.com/organizations/solven-eu/settings/apps/cleanthat
	// https://github.com/apps/cleanthat
	public static final String GITHUB_DEFAULT_APP_ID = "65550";

	// see org.kohsuke.github.extras.authorization.JWTTokenProvider.refreshJWT()
	// This will generate tokens valid for up to 2 minutes
	// JWTTokenProvider has its own cache mechanism
	final Supplier<JWTTokenProvider> appGithub = Suppliers.memoize(() -> {
		try {
			return makeJwtTokenProvider();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	});

	final Environment env;

	public GithubAppFactory(Environment env) {
		this.env = env;
	}

	@Override
	public GitHub makeAppGithub() {
		try {
			String jwt = makeJwt(appGithub.get());
			return new GitHubBuilder().withJwtToken(jwt)
					// This leads to 401. Why?
					// .withRateLimitChecker(new NoWaitRateLimitChecker())
					.withConnector(createGithubConnector())
					.build();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private ImmutableMap<String, GHPermissionType> getRequestedPermissions() {
		return ImmutableMap.<String, GHPermissionType>builder()
				// Required to access a repository without having to list all available repositories
				.put("pull_requests", GHPermissionType.WRITE)
				// Required to read files, and commit new versions
				.put("metadata", GHPermissionType.READ)
				// Required to commit cleaned files
				.put("contents", GHPermissionType.WRITE)
				// Required to edit the checks associated to the cleaning operation
				.put(GithubCheckRunManager.PERMISSION_CHECKS, GHPermissionType.WRITE)
				.build();
	}

	private GitHub makeInstallationGithub(GitHub github, String appToken) throws IOException {
		GitHubConnector ghConnector = createGithubConnector();
		return new GitHubBuilder().withEndpoint(github.getApiUrl())
				.withAppInstallationToken(appToken)
				.withConnector(ghConnector)
				.build();
	}

	public static GitHubConnector createGithubConnector() {
		// https://github.com/hub4j/github-api/issues/1202#issuecomment-890362069
		return new OkHttpGitHubConnector(new OkHttpClient());
		// return new HttpClientGitHubConnector();
	}

	@Deprecated
	public String makeJWT() throws JOSEException, IOException {
		JWTTokenProvider jwtTokenProvider = makeJwtTokenProvider();

		return makeJwt(jwtTokenProvider);
	}

	// https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature
	private String makeJwt(JWTTokenProvider jwtTokenProvider) throws IOException {
		String bearerToken = jwtTokenProvider.getEncodedAuthorization();

		if (!bearerToken.startsWith("Bearer ")) {
			throw new IllegalArgumentException("Invalid token, as it should start with 'Bearer '. It starts with "
					+ Ascii.truncate(bearerToken, PUBLIC_CHARS_IN_TOKEN, "..."));
		}
		var token = bearerToken.substring("Bearer ".length());

		return token;
		// // Create RSA-signer with the private key
		// JWSSigner signer = new RSASSASigner(rsaJWK);
		// // Prepare JWT with claims set
		// var now = new Date();
		//
		// //
		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-a-github-app
		// var expiresAt = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(GITHUB_TIMEOUT_JWK_MINUTES)
		// - TimeUnit.SECONDS.toMillis(GITHUB_TIMEOUT_SAFETY_SECONDS));
		// JWTClaimsSet claimsSet =
		// new JWTClaimsSet.Builder().issuer(githubAppId).issueTime(now).expirationTime(expiresAt).build();
		// SignedJWT signedJWT =
		// new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), claimsSet);
		// // Compute the RSA signature
		// signedJWT.sign(signer);
		// return signedJWT.serialize();
	}

	private JWTTokenProvider makeJwtTokenProvider() throws JOSEException {
		var rawJwk = env.getRequiredProperty(ENV_GITHUB_APP_PRIVATE_JWK);

		// if (rawJwk.equals(GithubWebhookHandlerFactory.GITHUB_APP_PRIVATE_JWK_FORUNITTESTS)
		// && GCInspector.inUnitTest()) {
		// LOGGER.info("We are in a unit-test");
		// return GithubWebhookHandlerFactory.GITHUB_APP_PRIVATE_JWK_FORUNITTESTS;
		// }

		RSAKey rsaJWK;
		try {
			rsaJWK = RSAKey.parse(rawJwk);
		} catch (IllegalStateException | ParseException e) {
			throw new IllegalStateException("Issue parsing privateKey", e);
		}

		var githubAppId = env.getProperty("github.app.app-id", GITHUB_DEFAULT_APP_ID);
		JWTTokenProvider jwtTokenProvider = new JWTTokenProvider(githubAppId, RSAKeyUtils.toRSAPrivateKey(rsaJWK));
		return jwtTokenProvider;
	}

	@Override
	public ResultOrError<GithubAndToken, WebhookRelevancyResult> makeInstallationGithub(long installationId) {
		try {
			GitHub github = makeAppGithub();
			GHAppInstallation installationById = github.getApp().getInstallationById(installationId);
			Map<String, GHPermissionType> availablePermissions = installationById.getPermissions();

			// This check is dumb, as we should also compare the values
			Map<String, GHPermissionType> requestedPermissions = getRequestedPermissions();
			if (!availablePermissions.keySet().containsAll(requestedPermissions.keySet())) {
				return ResultOrError.error(
						WebhookRelevancyResult.dismissed("We lack proper permissions. Available=" + availablePermissions
								+ " vs requested="
								+ requestedPermissions));
			}

			Map<String, GHPermissionType> permissions = availablePermissions;
			LOGGER.info("Permissions: {}", permissions);
			LOGGER.info("RepositorySelection: {}", installationById.getRepositorySelection());
			// https://github.com/hub4j/github-api/issues/570
			// Required to open new pull-requests
			GHAppCreateTokenBuilder installationGithubBuilder =
					installationById.createToken().permissions(requestedPermissions);

			GHAppInstallationToken installationToken;
			try {
				// https://github.com/hub4j/github-api/issues/570
				installationToken = installationGithubBuilder.create();
			} catch (HttpException e) {
				if (e.getMessage().contains("The permissions requested are not granted to this installation.")) {
					LOGGER.trace("Lack proper permissions", e);
					return ResultOrError.error(WebhookRelevancyResult
							.dismissed("We lack proper permissions. Available=" + availablePermissions));
				} else {
					throw new UncheckedIOException(e);
				}
			}

			String appToken = installationToken.getToken();
			GitHub installationGithub = makeInstallationGithub(github, appToken);

			// https://stackoverflow.com/questions/45427275/how-to-check-my-github-current-rate-limit
			LOGGER.info("Initialized an installation github. RateLimit status: {}", installationGithub.getRateLimit());
			return ResultOrError
					.result(new GithubAndToken(installationGithub, appToken, installationById, permissions));
		} catch (GHFileNotFoundException e) {
			throw new UncheckedIOException("Invalid installationId, or no actual access to it?", e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
