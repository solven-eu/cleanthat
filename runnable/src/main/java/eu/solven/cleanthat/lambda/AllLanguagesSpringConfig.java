package eu.solven.cleanthat.lambda;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.language.java.JavaFormatter;
import eu.solven.cleanthat.language.json.JsonFormatter;

/**
 * Spring configuration wrapping all available languages
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ JavaFormatter.class, JsonFormatter.class })
public class AllLanguagesSpringConfig {

}
