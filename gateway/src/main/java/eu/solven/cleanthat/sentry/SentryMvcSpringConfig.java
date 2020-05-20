package eu.solven.cleanthat.sentry;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.HandlerExceptionResolver;

import io.sentry.spring.SentryExceptionResolver;
import io.sentry.spring.SentryServletContextInitializer;

/**
 * Add Sentry specific behavior for Spring MVC
 * 
 * @author Benoit Lacelle
 *
 */
@Import(SentryCoreSpringConfig.class)
@Configuration
public class SentryMvcSpringConfig implements ISentrySpringConfig {

	// https://docs.sentry.io/clients/java/modules/spring/#recording-exceptions
	@Bean
	public HandlerExceptionResolver sentryExceptionResolver() {
		return new SentryExceptionResolver();
	}

	// https://docs.sentry.io/clients/java/modules/spring/#spring-boot-http-data
	@Bean
	public ServletContextInitializer sentryServletContextInitializer() {
		return new SentryServletContextInitializer();
	}
}
