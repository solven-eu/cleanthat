package eu.solven.cleanthat.github;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.lambda.step0_checkwebhook.CheckWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step2_executeclean.ExecuteCleaningWebhooksLambdaFunction;

/**
 * Equivalent of {@link CheckWebhooksLambdaFunction}, but in the test classpath, which has
 * spring-cloud-function-starter-web in addition
 *
 * @author Benoit Lacelle
 */
public class DebugExecuteCleaningWebhooksLambdaFunction extends ExecuteCleaningWebhooksLambdaFunction
		implements CommandLineRunner {

	@Autowired
	ApplicationContext appContext;

	public static void main(String[] args) {
		SpringApplication.run(DebugExecuteCleaningWebhooksLambdaFunction.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Function<Map<String, ?>, ?> function = appContext.getBean("ingressRawWebhook", Function.class);

		function.apply(new ObjectMapper().readValue(
				new ClassPathResource("/github/webhook/pr_open-open_event.json").getInputStream(),
				Map.class));
	}
}
