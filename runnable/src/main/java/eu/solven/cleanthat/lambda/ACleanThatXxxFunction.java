package eu.solven.cleanthat.lambda;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import com.nimbusds.jose.JOSEException;

import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.GithubSpringConfig;
import eu.solven.cleanthat.github.event.GithubPullRequestCleaner;
import eu.solven.cleanthat.github.event.GithubWebhookHandlerFactory;
import io.sentry.Sentry;

/**
 * Based class for Lambda, Functions, etc
 * 
 * @author Benoit Lacelle
 *
 */
@SpringBootApplication(scanBasePackages = "none")
@Import({ GithubSpringConfig.class, JavaFormatter.class })
public class ACleanThatXxxFunction {
	public static void main(String[] args) {
		SpringApplication.run(ACleanThatXxxFunction.class, args);
	}

	protected Map<String, ?> processOneMessage(ApplicationContext appContext, Map<String, ?> input) {
		GithubWebhookHandlerFactory githubFactory = appContext.getBean(GithubWebhookHandlerFactory.class);
		GithubPullRequestCleaner cleaner = appContext.getBean(GithubPullRequestCleaner.class);

		try {
			// TODO Cache the Github instance for the JWT duration
			return githubFactory.makeWithFreshJwt().processWebhookBody(input, cleaner);
		} catch (IOException | JOSEException | RuntimeException e) {
			Sentry.captureException(e, "Lambda");
			throw new RuntimeException(e);
		}
	}
}
