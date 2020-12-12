package eu.solven.cleanthat.gateway;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.gateway.lambda.CleanThatLambdaInvoker;
import eu.solven.cleanthat.github.GithubController;

/**
 * Main SpringBoot {@link Configuration}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({
		// MVC
		CleanThatMvcConfigurer.class,

		GithubController.class,
		CleanThatLambdaInvoker.class, })
public class CleanThatSpringConfig {
}
