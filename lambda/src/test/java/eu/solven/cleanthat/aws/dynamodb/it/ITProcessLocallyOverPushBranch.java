package eu.solven.cleanthat.aws.dynamodb.it;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.lambda.step0_checkwebhook.CheckWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.CheckConfigWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step2_executeclean.ExecuteCleaningWebhooksLambdaFunction;

/**
 * This enables processing similarly that on a push event over given branch
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated(since = "NotReady: it is complicated to craft GitHub events manually (installation.id, after.sha1, ...)")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CheckWebhooksLambdaFunction.class,
		CheckConfigWebhooksLambdaFunction.class,
		ExecuteCleaningWebhooksLambdaFunction.class })
public class ITProcessLocallyOverPushBranch {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITProcessLocallyOverPushBranch.class);

	@Autowired
	CheckWebhooksLambdaFunction checkEvent;
	@Autowired
	CheckConfigWebhooksLambdaFunction checkConfig;
	@Autowired
	ExecuteCleaningWebhooksLambdaFunction cleanCode;

	@Test
	public void testInitWithDefaultConfiguration() throws IOException, JOSEException {
		Map<String, ?> githubEvent =
				ImmutableMap.<String, Object>builder().put("installation", Map.of("id", 123L)).build();
		Map<String, ?> checkEventOutput = checkEvent.ingressRawWebhook().apply(githubEvent);
		Map<String, ?> checkConfigOutput = checkConfig.ingressRawWebhook().apply(checkEventOutput);
		Map<String, ?> cleanCodeOutput = cleanCode.ingressRawWebhook().apply(checkConfigOutput);

		LOGGER.info("Output: {}", cleanCodeOutput);
	}
}
