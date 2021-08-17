package eu.solven.cleanthat.lambda;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public abstract class ACleanThatXxxApplication implements ApplicationContextAware {

	ApplicationContext appContext;

	@Override
	public void setApplicationContext(ApplicationContext appContext) {
		this.appContext = appContext;
	}

	public ApplicationContext getAppContext() {
		return appContext;
	}

}
