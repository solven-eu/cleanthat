package eu.solven.cleanthat.lambda;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.config.GitService;

/**
 * Spring configuration wrapping technical helpers
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({
		// Technical Beans
		GitService.class })
public class TechnicalBoilerplateSpringConfig {

}
