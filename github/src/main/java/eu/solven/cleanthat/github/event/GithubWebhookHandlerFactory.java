package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Factory for {@link GitHub}, on a per-installation basis
 *
 * @author Benoit Lacelle
 */
public class GithubWebhookHandlerFactory {

	// https://github.com/organizations/solven-eu/settings/apps/cleanthat
	// https://github.com/apps/cleanthat
	public static final String GITHUB_DEFAULT_APP_ID = "65550";

	// https://github.community/t/expiration-time-claim-exp-is-too-far-in-the-future-when-creating-an-access-token/13830
	private static final int GITHUB_TIMEOUT_SAFETY_SECONDS = 15;

	private static final int GITHUB_TIMEOUT_JWK_MINUTES = 10;

	final Environment env;

	final ObjectMapper objectMapper;

	public GithubWebhookHandlerFactory(Environment env, ObjectMapper objectMapper) {
		this.env = env;
		this.objectMapper = objectMapper;
	}

	public IGithubWebhookHandler makeWithFreshJwt() throws IOException, JOSEException {
		GitHub github = new GitHubBuilder().withJwtToken(makeJWT()).build();
		return new GithubWebhookHandler(github, objectMapper);
	}

	// https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature
	public String makeJWT() throws JOSEException {
		String rawJwk = env.getRequiredProperty("github.app.private-jwk");
		RSAKey rsaJWK;
		try {
			rsaJWK = RSAKey.parse(rawJwk);
		} catch (IllegalStateException | ParseException e) {
			throw new IllegalStateException("Issue parsing privateKey", e);
		}
		// Create RSA-signer with the private key
		JWSSigner signer = new RSASSASigner(rsaJWK);
		// Prepare JWT with claims set
		Date now = new Date();

		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-a-github-app
		Date expiresAt = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(GITHUB_TIMEOUT_JWK_MINUTES)
				- TimeUnit.SECONDS.toMillis(GITHUB_TIMEOUT_SAFETY_SECONDS));
		String githubAppId = env.getProperty("github.app.app-id", GITHUB_DEFAULT_APP_ID);
		JWTClaimsSet claimsSet =
				new JWTClaimsSet.Builder().issuer(githubAppId).issueTime(now).expirationTime(expiresAt).build();
		SignedJWT signedJWT =
				new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), claimsSet);
		// Compute the RSA signature
		signedJWT.sign(signer);
		return signedJWT.serialize();
	}
}
