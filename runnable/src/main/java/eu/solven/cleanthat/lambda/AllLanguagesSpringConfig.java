package eu.solven.cleanthat.lambda;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.language.java.JavaFormattersFactory;
import eu.solven.cleanthat.language.scala.ScalaFormattersFactory;

/**
 * Spring configuration wrapping all available languages
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ JavaFormattersFactory.class, ScalaFormattersFactory.class })
public class AllLanguagesSpringConfig {

}
