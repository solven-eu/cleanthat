package eu.solven.cleanthat.gateway;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.solven.cleanthat.sentry.SentryMvcSpringConfig;

/**
 * Main SpringBoot {@link Configuration}
 * 
 * @author Benoit Lacelle
 *
 */
@Configuration
@Import({
		// Security
		// AgileaSecurityConfig.class,

		// MVC
		CleanThatMvcConfigurer.class,
		// AgileaSwaggerMvcConfigurer.class,
		// AgileaExceptionAdvisor.class,

		// SQL
		// AgileaPgsqlConfig.class,

		// GraphQL
		// AgileaGraphQLSpringConfig.class,

		// Encryption
		// AgileaEncrypter.class,
		// AgileaDecrypter.class,

		// Monitoring
		SentryMvcSpringConfig.class, })
@ComponentScan(basePackages = { "eu.agilea.pgsql.bridges", "eu.agilea.mvc.controllers", "eu.agilea.services" })
public class CleanThatSpringConfig {
	// @Bean
	// public ObjectMapper objectMapper() {
	// return AgileaJsonHelpers.makeObjectMapper();
	// }
}
