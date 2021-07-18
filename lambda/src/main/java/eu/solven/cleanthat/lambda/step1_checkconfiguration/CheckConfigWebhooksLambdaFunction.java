package eu.solven.cleanthat.lambda.step1_checkconfiguration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.github.event.pojo.CleanThatWebhookEvent;
import eu.solven.cleanthat.github.event.pojo.WebhookRelevancyResult;
import eu.solven.cleanthat.lambda.ACleanThatXxxFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.CheckWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * Used to check if given webhook is associated to a valid configuration (e.g. to filter irrelevant repositories).
 * 
 * @author Benoit Lacelle
 *
 */
public class CheckConfigWebhooksLambdaFunction extends ACleanThatXxxFunction {
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckConfigWebhooksLambdaFunction.class);

	@Override
	protected Map<String, ?> unsafeProcessOneEvent(ApplicationContext appContext, IWebhookEvent input) {
		GithubWebhookHandlerFactory githubFactory = appContext.getBean(GithubWebhookHandlerFactory.class);

		// TODO Cache the Github instance for the JWT duration
		IGithubWebhookHandler makeWithFreshJwt;
		try {
			makeWithFreshJwt = githubFactory.makeWithFreshJwt();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

		ICodeCleanerFactory cleanerFactory = getAppContext().getBean(ICodeCleanerFactory.class);

		WebhookRelevancyResult processAnswer =
				makeWithFreshJwt.filterWebhookEventTargetRelevantBranch(cleanerFactory, input);

		if (processAnswer.getOptBranchToClean().isPresent()) {
			AmazonDynamoDB client = CheckWebhooksLambdaFunction.makeDynamoDbClient();

			Map<String, Object> acceptedEvent = new LinkedHashMap<>(input.getBody());

			acceptedEvent.put("refToClean", processAnswer.getOptBranchToClean().get());

			CheckWebhooksLambdaFunction.saveToDynamoDb("cleanthat_accepted_events",
					new CleanThatWebhookEvent(input.getHeaders(), acceptedEvent),
					client);
		} else {
			LOGGER.info("Rejected due to: {}", processAnswer.getOptRejectedReason().get());
		}

		return Map.of("whatever", "done");
	}

}
