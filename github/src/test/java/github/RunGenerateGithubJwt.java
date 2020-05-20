package github;

import java.io.File;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import com.google.common.io.Files;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

// https://github-api.kohsuke.org/githubappjwtauth.html
public class RunGenerateGithubJwt {
	// https://github.com/organizations/solven-eu/settings/apps/cleanthat
	// https://github.com/apps/cleanthat
	public static final String githubAppId = "65550";

	static PrivateKey get(String filename) throws Exception {
		byte[] keyBytes = Files.toByteArray(new File(filename));

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	static String createJWT(String githubAppId, long ttlMillis) throws Exception {
		// The JWT signature algorithm we will be using to sign the token
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);

		// We will sign our JWT with our private key
		Key signingKey = get("~/Dropbox/Solven/Dev/CleanThat/github-api-app.private-key.der");

		// Let's set the JWT Claims
		JwtBuilder builder =
				Jwts.builder().setIssuedAt(now).setIssuer(githubAppId).signWith(signingKey, signatureAlgorithm);

		// if it has been specified, let's add the expiration
		if (ttlMillis > 0) {
			long expMillis = nowMillis + ttlMillis;
			Date exp = new Date(expMillis);
			builder.setExpiration(exp);
		}

		// Builds the JWT and serializes it to a compact, URL-safe string
		return builder.compact();
	}

	public static void main(String[] args) throws Exception {
		String jwtToken = createJWT(githubAppId, 600000);
		GitHub gitHubApp = new GitHubBuilder().withJwtToken(jwtToken).build();

		GHAppInstallation appInstallation = gitHubApp.getApp().getInstallationById(111111);
	}
}
