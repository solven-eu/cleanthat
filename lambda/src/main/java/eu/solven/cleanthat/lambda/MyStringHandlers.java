package eu.solven.cleanthat.lambda;

import java.util.Map;

import org.springframework.cloud.function.adapter.aws.SpringBootRequestHandler;

/**
 * Beware a lambda does not need an HTTP server: it can be exclusive to processing events, or files in S3. This will
 * enable AWS to receive events through API calls
 *
 * @author Benoit Lacelle
 */
public class MyStringHandlers extends SpringBootRequestHandler<Map<String, ?>, Map<String, ?>> {
	public MyStringHandlers() {
		// We should rely on META-INF/MANIFEST.MF, but sometimes, it does not work: we then declare the function
		// explicitly
		super(CleanThatLambdaFunction.class);
	}
}
