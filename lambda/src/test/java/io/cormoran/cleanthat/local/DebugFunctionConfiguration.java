package io.cormoran.cleanthat.local;

import eu.solven.cleanthat.lambda.CleanThatLambdaFunction;

/**
 * Equivalent of {@link CleanThatLambdaFunction}, but in the test classpath, which has spring-cloud-function-starter-web
 * in addition
 *
 * @author Benoit Lacelle
 */
public class DebugFunctionConfiguration extends CleanThatLambdaFunction {

	public static void main(String[] args) {
		CleanThatLambdaFunction.main(args);
	}
}
