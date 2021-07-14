package eu.solven.cleanthat.lambda;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.GithubSpringConfig;

/**
 * Based class for any application
 * 
 * @author Benoit Lacelle
 *
 */
@SpringBootApplication(scanBasePackages = "none")
@Import({ GithubSpringConfig.class, JavaFormatter.class, CodeProviderHelpers.class })
public abstract class ACleanThatXxxApplication {

	@Autowired
	ApplicationContext appContext;

	// protected abstract boolean isAccepted(IGithubWebhookHandler makeWithFreshJwt, Map<String, ?> input);
	//
	// protected abstract void onAccepted(IGithubWebhookHandler makeWithFreshJwt, Map<String, ?> input);

	public ApplicationContext getAppContext() {
		return appContext;
	}

}
