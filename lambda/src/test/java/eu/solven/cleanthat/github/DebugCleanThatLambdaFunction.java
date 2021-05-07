package eu.solven.cleanthat.github;

import eu.solven.cleanthat.lambda.CleanThatWebhookLambdaFunction;

/**
 * Equivalent of {@link CleanThatWebhookLambdaFunction}, but in the test classpath, which has
 * spring-cloud-function-starter-web in addition
 *
 * @author Benoit Lacelle
 */
public class DebugCleanThatLambdaFunction extends CleanThatWebhookLambdaFunction {

	public static void main(String[] args) {
		CleanThatWebhookLambdaFunction.main(args);
	}
}
