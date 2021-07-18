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
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class ACleanThatXxxApplication {

	@Autowired
	ApplicationContext appContext;

	public ApplicationContext getAppContext() {
		return appContext;
	}

}
