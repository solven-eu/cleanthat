package eu.solven.cleanthat.lambda;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.language.java.JavaFormatter;
import eu.solven.cleanthat.language.scala.ScalaFormatter;

/**
 * Spring configuration wrapping all available languages
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ JavaFormatter.class, ScalaFormatter.class })
public class AllLanguagesSpringConfig {

}
