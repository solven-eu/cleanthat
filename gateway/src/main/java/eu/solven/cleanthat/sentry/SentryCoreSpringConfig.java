package eu.solven.cleanthat.sentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * @see https://sentry.io/onboarding/m-itrust/datasharing/configure/java
 * @author Benoit Lacelle
 */
@Configuration
public class SentryCoreSpringConfig implements ISentrySpringConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(SentryCoreSpringConfig.class);

	// TODO git.properties is not generated in Heroku as .git is removed by Heroku
	// https://stackoverflow.com/questions/14583282/heroku-display-hash-of-current-commit/34536363#34536363
	// heroku labs:enable runtime-dyno-metadata -a agilea-app
	// String release = env.getProperty("heroku.slug.commit", "not_heroku");

}
