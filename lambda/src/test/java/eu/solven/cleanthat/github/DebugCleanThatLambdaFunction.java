package eu.solven.cleanthat.github;

import eu.solven.cleanthat.lambda.step0_checkwebhook.CheckWebhooksLambdaFunction;

/**
 * Equivalent of {@link CheckWebhooksLambdaFunction}, but in the test classpath, which has
 * spring-cloud-function-starter-web in addition
 *
 * @author Benoit Lacelle
 */
public class DebugCleanThatLambdaFunction extends CheckWebhooksLambdaFunction {

	public static void main(String[] args) {
		CheckWebhooksLambdaFunction.main(args);
	}
}
