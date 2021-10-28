package eu.solven.cleanthat.lambda;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	protected final Map<String, ?> processOneEvent(IWebhookEvent input) {
		try {
			return unsafeProcessOneEvent(input);
		} catch (RuntimeException e) {
			Map<String, ?> body = input.getBody();

			try {
				LOGGER.warn("Issue with IWebhookEvent. body={}", new ObjectMapper().writeValueAsString(body));
			} catch (JsonProcessingException e1) {
				LOGGER.warn("Issue printing as json. body: {}", body);
			}

			RuntimeException wrapped = new RuntimeException(e);
			Sentry.captureException(wrapped, "Lambda");

			throw wrapped;
		}
	}

	protected abstract Map<String, ?> unsafeProcessOneEvent(IWebhookEvent input);

}
