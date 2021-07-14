package eu.solven.cleanthat.lambda;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import io.sentry.Sentry;

/**
 * Based class for Lambda, Functions, etc
 * 
 * @author Benoit Lacelle
 *
 */
public abstract class ACleanThatXxxFunction extends ACleanThatXxxApplication {

	protected final Map<String, ?> processOneEvent(ApplicationContext appContext, IWebhookEvent input) {
		try {
			return unsafeProcessOneEvent(appContext, input);
		} catch (RuntimeException e) {
			RuntimeException wrapped = new RuntimeException(e);
			Sentry.captureException(wrapped, "Lambda");
			throw wrapped;
		}
	}

	protected abstract Map<String, ?> unsafeProcessOneEvent(ApplicationContext appContext, IWebhookEvent input);

}
