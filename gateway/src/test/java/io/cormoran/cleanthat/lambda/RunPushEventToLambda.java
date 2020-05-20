package io.cormoran.cleanthat.lambda;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.gateway.lambda.CleanThatLambdaInvoker;
import eu.solven.cleanthat.github.GoogleController;

public class RunPushEventToLambda {
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		// Set as environment variable: AWS_LAMBDA_INVOKE_ACCESS_KEY
		// Set as environment variable: AWS_LAMBDA_INVOKE_SECRET_KEY
		Environment env = new StandardEnvironment();

		ObjectMapper objectMapper = new ObjectMapper();
		CleanThatLambdaInvoker lambdaInvoker = new CleanThatLambdaInvoker(env, objectMapper);
		GoogleController googleController = new GoogleController(lambdaInvoker);

		googleController.onWebhook(
				objectMapper.readValue(new ClassPathResource("/github/webhook.push.json").getInputStream(), Map.class));
	}
}
