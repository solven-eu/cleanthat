package eu.solven.cleanthat.lambda;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;

/**
 * Based class for any application
 * 
 * @author Benoit Lacelle
 *
 */
@SpringBootApplication(scanBasePackages = "none")
@Import({ GithubSpringConfig.class, AllLanguagesSpringConfig.class, CodeProviderHelpers.class })
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class ACleanThatXxxApplication {

	@Autowired
	ApplicationContext appContext;

	public ApplicationContext getAppContext() {
		return appContext;
	}

}
