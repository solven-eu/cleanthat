package eu.solven.cleanthat.github.run;

import java.io.File;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import org.kohsuke.github.GHApp;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.StandardCharset;

import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;

// https://github-api.kohsuke.org/githubappjwtauth.html
public class RunGenerateGithubJwt {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunGenerateGithubJwt.class);

	static PrivateKey get(String filename) throws Exception {
		byte[] keyBytes = Files.toByteArray(new File(filename));
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	public static void main(String[] args) throws Exception {
		JWK jwk = JWK.parseFromPEMEncodedObjects(Files.asCharSource(
				new File(System.getProperty("user.home")
						+ "/Dropbox/Solven/Dev/CleanThat/cleanthat.2020-05-19.private-key.pem"),
				StandardCharset.UTF_8).read());
		LOGGER.info("github.app.private-jwk: '{}'", jwk.toJSONString());
		MockEnvironment env = new MockEnvironment();
		env.setProperty("github.app.app-id", GithubWebhookHandlerFactory.GITHUB_DEFAULT_APP_ID);
		env.setProperty("github.app.private-jwk", jwk.toJSONString());
		GithubWebhookHandlerFactory factory = new GithubWebhookHandlerFactory(env, new ObjectMapper());
		GitHub gitHubApp = factory.makeWithFreshJwt().getGithubAsApp();
		GHApp app = gitHubApp.getApp();
		app.listInstallations().forEach(install -> {
			LOGGER.info("appId={} url={}", install.getId(), install.getHtmlUrl());
		});
	}
}
