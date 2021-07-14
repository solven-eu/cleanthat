package eu.solven.cleanthat.lambda.step1_checkconfiguration;

import java.util.Map;

import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

/**
 * Beware a lambda does not need an HTTP server: it can be exclusive to processing events, or files in S3. This will
 * enable AWS to receive events through API calls
 *
 * @author Benoit Lacelle
 */
public class CheckConfigWebhooksHandler extends SpringBootRequestHandler<Map<String, ?>, Map<String, ?>> {
	public CheckConfigWebhooksHandler() {
		// We declare the function explicitly as given jar holds multiple lambda functions
		super(CheckConfigWebhooksLambdaFunction.class);
	}
}
