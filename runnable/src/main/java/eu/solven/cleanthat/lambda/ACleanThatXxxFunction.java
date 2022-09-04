package eu.solven.cleanthat.lambda;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;
import io.sentry.Sentry;

/**
 * Based class for Lambda, Functions, etc
 * 
 * @author Benoit Lacelle
 *
 */
public abstract class ACleanThatXxxFunction extends ACleanThatXxxApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(ACleanThatXxxFunction.class);

	static {
		// https://stackoverflow.com/questions/35298616/aws-lambda-and-inaccurate-memory-allocation
		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		LOGGER.info("Input arguments: {}", inputArguments);
	}

	protected final Map<String, ?> processOneEvent(IWebhookEvent input) {
		try {
			return unsafeProcessOneEvent(input);
		} catch (RuntimeException e) {
			// Headers should hold the eventId,which enables fetching it from DB
			LOGGER.warn("Issue processing an event. headers=" + input.getHeaders(), e);

			RuntimeException wrapped = new RuntimeException(e);
			Sentry.captureException(wrapped, "Lambda");

			throw wrapped;
		}
	}

	protected abstract Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input);

}
