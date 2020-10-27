package eu.solven.cleanthat.sentry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sentry.Sentry;
import io.sentry.SentryClient;

/**
 * @see https://sentry.io/onboarding/m-itrust/datasharing/configure/java
 * @author Benoit Lacelle
 */
@Configuration
public class SentryCoreSpringConfig implements ISentrySpringConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(SentryCoreSpringConfig.class);

	@Bean
	public SentryClient sentryClient(Environment env) {
		String sentryDsn = env.getRequiredProperty("sentry.dsn");
		LOGGER.info("Initializing Sentry with DSN: {}", sentryDsn);
		SentryClient sentryClient = Sentry.init(sentryDsn);
		// TODO git.properties is not generated in Heroku as .git is removed by Heroku
		// https://stackoverflow.com/questions/14583282/heroku-display-hash-of-current-commit/34536363#34536363
		// heroku labs:enable runtime-dyno-metadata -a agilea-app
		String release = env.getProperty("heroku.slug.commit", "not_heroku");
		LOGGER.info("release={}", release);
		sentryClient.setRelease(release);
		// https://devcenter.heroku.com/articles/dynos#local-environment-variables
		String serverName = env.getProperty("dyno", "not_heroku");
		LOGGER.info("server_name={}", serverName);
		sentryClient.setServerName(serverName);
		return sentryClient;
	}

	@ConditionalOnBean(EventBus.class)
	@Bean
	public Object sentryExceptionListener(EventBus eventBus, SentryClient sentry) {
		Object exceptionListener = new Object() {

			@SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS",
					justification = "EventBus will introspect and find this method")
			@Subscribe
			public void onException(Throwable t) {
				sentry.sendException(t);
			}
		};
		eventBus.register(exceptionListener);
		return exceptionListener;
	}
}
