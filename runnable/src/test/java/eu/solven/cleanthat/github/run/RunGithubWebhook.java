package eu.solven.cleanthat.github.run;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.lambda.ACleanThatXxxFunction;

public class RunGithubWebhook extends ACleanThatXxxFunction {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunGithubWebhook.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(RunGithubWebhook.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	public static class CleanThatLocalFunction extends ACleanThatXxxFunction {
		@Override
		public Map<String, ?> processOneMessage(ApplicationContext appContext, Map<String, ?> input) {
			return super.processOneMessage(appContext, input);
		}
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup(ContextRefreshedEvent event) throws IOException, JOSEException {
		ApplicationContext appContext = event.getApplicationContext();

		Map<String, ?> input = new ObjectMapper()
				.readValue(new ClassPathResource("/github/webhook.commit.json").getInputStream(), Map.class);

		Map<String, ?> output = new CleanThatLocalFunction().processOneMessage(appContext, input);

		LOGGER.info("Output: {}", output);
	}
}
