package io.cormoran.cleanthat;

import io.cormoran.cleanthat.lambda.FunctionConfiguration;

/**
 * Equivalent of {@link FunctionConfiguration}, but in the test classpath, which has spring-cloud-function-starter-web
 * in addition
 * 
 * @author Benoit Lacelle
 *
 */
public class DebugFunctionConfiguration extends FunctionConfiguration {

	public static void main(String[] args) {
		FunctionConfiguration.main(args);
	}
}