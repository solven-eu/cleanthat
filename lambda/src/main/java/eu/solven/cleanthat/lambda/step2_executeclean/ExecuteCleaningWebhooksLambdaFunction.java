package eu.solven.cleanthat.lambda.step2_executeclean;

import java.util.Map;

import eu.solven.cleanthat.code_provider.github.event.CompositeCodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.code_provider.github.event.IGitWebhookHandler;
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
		IGitWebhookHandler makeWithFreshJwt = CheckConfigWebhooksLambdaFunction.extracted(getAppContext());
		ICodeCleanerFactory cleanerFactory = getAppContext().getBean(CompositeCodeCleanerFactory.class);

		makeWithFreshJwt.doExecuteWebhookEvent(cleanerFactory, input);

		return Map.of("whatever", "done");
	}

}
