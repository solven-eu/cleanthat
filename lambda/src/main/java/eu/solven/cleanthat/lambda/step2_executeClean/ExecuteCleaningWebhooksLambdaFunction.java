package eu.solven.cleanthat.lambda.step2_executeClean;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import eu.solven.cleanthat.github.event.ICodeCleanerFactory;
import eu.solven.cleanthat.github.event.IGithubWebhookHandler;
import eu.solven.cleanthat.lambda.ACleanThatXxxFunction;
import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * Used to actually execute the cleaning
 * 
 * @author Benoit Lacelle
 *
 */
public class ExecuteCleaningWebhooksLambdaFunction extends ACleanThatXxxFunction {
	// private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteCleaningWebhooksLambdaFunction.class);

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

		ICodeCleanerFactory cleaner = getAppContext().getBean(ICodeCleanerFactory.class);

		makeWithFreshJwt.doExecuteWebhookEvent(cleaner, input);

		return Map.of("whatever", "done");
	}

}
