package eu.solven.cleanthat.github;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.lambda.step0_checkwebhook.CheckWebhooksLambdaFunction;
import eu.solven.cleanthat.lambda.step1_checkconfiguration.CheckConfigWebhooksLambdaFunction;

/**
 * Equivalent of {@link CheckWebhooksLambdaFunction}, but in the test classpath, which has
 * spring-cloud-function-starter-web in addition
 *
 * @author Benoit Lacelle
 */
public class DebugCheckWebhooksLambdaFunction extends CheckConfigWebhooksLambdaFunction implements CommandLineRunner {

	@Autowired
	ApplicationContext appContext;

	public static void main(String[] args) {
		SpringApplication.run(DebugCheckWebhooksLambdaFunction.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Function<Map<String, ?>, ?> function = appContext.getBean("ingressRawWebhook", Function.class);

		Map<String, ?> githubEvent = new ObjectMapper().readValue(
				new ClassPathResource("/github/webhook/pr_open-open_event.json").getInputStream(),
				Map.class);

		Map<String, Object> inputAsMap = new LinkedHashMap<>();
		inputAsMap.put("body", Map.of("github", Map.of("body", githubEvent, "headers", Map.of())));
		inputAsMap.put("headers", Map.of());

		function.apply(inputAsMap);
	}
}
