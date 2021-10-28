package eu.solven.cleanthat.mvn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import io.sentry.IHub;

/**
 * Mojo relying on a Spring {@link ApplicationContext}
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
public abstract class ACleanThatSpringMojo extends ACleanThatMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatInitMojo.class);

	protected static final AtomicReference<ACleanThatSpringMojo> CURRENT_MOJO = new AtomicReference<>();

	/**
	 * The SpringBoot application started within maven Mojo
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	@SpringBootApplication(scanBasePackages = "none")
	public static class MavenSpringConfig implements CommandLineRunner {

		@Autowired
		ApplicationContext appContext;

		@Override
		public void run(String... args) throws Exception {
			LOGGER.info("Processing arguments: {}", Arrays.asList(args));

			// Ensure events are sent to Sentry
			IHub sentryHub = appContext.getBean(IHub.class);
			sentryHub.captureMessage("Maven is OK");

			CURRENT_MOJO.get().doClean(appContext);
			sentryHub.flush(TimeUnit.SECONDS.toMillis(1));
		}
	}

	// Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html
	@Override
	public void execute() throws MojoExecutionException {
		getLog().debug("Hello, world.");
		checkParameters();

		if (CURRENT_MOJO.compareAndSet(null, this)) {
			LOGGER.info("Start applicationContext");
			try {
				List<Class<?>> classes = new ArrayList<>();

				classes.add(MavenSpringConfig.class);
				classes.addAll(springClasses());

				SpringApplication.run(classes.toArray(Class<?>[]::new), new String[0]);
			} finally {
				LOGGER.info("Closed applicationContext");
				// Beware to clean so that it is OK in a multiModule reactor
				CURRENT_MOJO.set(null);
			}
		} else {
			throw new IllegalStateException("We have a leftover Mojo");
		}
	}

	protected abstract void doClean(ApplicationContext appContext) throws IOException;

	protected abstract List<? extends Class<?>> springClasses();
}