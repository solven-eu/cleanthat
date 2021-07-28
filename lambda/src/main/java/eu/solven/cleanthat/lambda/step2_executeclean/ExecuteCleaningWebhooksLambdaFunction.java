package eu.solven.cleanthat.lambda.step2_executeclean;

import java.util.Map;

import eu.solven.cleanthat.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.lambda.AWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.CheckConfigWebhooksLambdaFunction;

/**
 * Used to actually execute the cleaning
 * 
 * @author Benoit Lacelle
 *
 */
public class ExecuteCleaningWebhooksLambdaFunction extends AWebhooksLambdaFunction {
	// private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteCleaningWebhooksLambdaFunction.class);

	@Override
	protected Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input) {
		IGithubWebhookHandler makeWithFreshJwt = CheckConfigWebhooksLambdaFunction.extracted(getAppContext());
		ICodeCleanerFactory cleanerFactory = getAppContext().getBean(ICodeCleanerFactory.class);

		makeWithFreshJwt.doExecuteWebhookEvent(cleanerFactory, input);

		return Map.of("whatever", "done");
	}

}
