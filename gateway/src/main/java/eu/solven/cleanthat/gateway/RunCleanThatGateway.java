package eu.solven.cleanthat.gateway;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main Entry-point for the back-end
 * 
 * @author Benoit Lacelle
 *
 */
// https://stackoverflow.com/questions/40228036/how-to-turn-off-spring-security-in-spring-boot-applicationot
@SpringBootApplication(
		exclude = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
		scanBasePackages = "noscan")
public class RunCleanThatGateway {
	public static final String KEY_SPRING_PROFILES_ACTIVE = "spring.profiles.active";

	private static final Logger LOGGER = LoggerFactory.getLogger(RunCleanThatGateway.class);

	protected RunCleanThatGateway() {
		// hidden
	}

	private static void handleSlf4jBridge() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	public static void main(String[] args) {
		handleSlf4jBridge();

		ApplicationContext context = new RunCleanThatGateway().start(args);

		LOGGER.info("Started: {}", context);
	}

	public ConfigurableApplicationContext start(String... args) {
		List<Class<?>> sources = getSpringConfigurations(true);

		SpringApplication app = new SpringApplication(sources.toArray(new Class[0]));

		return app.run(args);
	}

	/**
	 * 
	 * @param withSwagger
	 *            unit-tests often does not like Swagger
	 * @return
	 */
	public List<Class<?>> getSpringConfigurations(boolean withSwagger) {
		List<Class<?>> sources = new ArrayList<>();

		// Main entry point
		sources.add(RunCleanThatGateway.class);

		sources.add(CleanThatSpringConfig.class);

		// if (withSwagger) {
		// check https://root/swagger-ui.html
		// sources.addAll(Arrays.asList(AgileaSwaggerMvcConfigurer.class));
		// }

		return sources;
	}

}
