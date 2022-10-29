package eu.solven.cleanthat.lambda;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.language.java.JavaFormattersFactory;
import eu.solven.cleanthat.language.json.JsonFormattersFactory;
import eu.solven.cleanthat.language.xml.XmlFormattersFactory;

/**
 * Spring configuration wrapping all available languages
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({ JavaFormattersFactory.class, JsonFormattersFactory.class, XmlFormattersFactory.class })
public class AllLanguagesSpringConfig {

	// TODO Rely on AutoConfiguration
	// Scala is typically excluded from packaes (e.g. Lambda due to size limitations
	// @ConditionalOnClass(ScalaFormattersFactory.class)
	// @Bean
	// public ScalaFormattersFactory ScalaFormattersFactory(ObjectMapper objectMapper) {
	// return new ScalaFormattersFactory(objectMapper);
	// }
}
