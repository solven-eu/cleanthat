package eu.solven.cleanthat.mvn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.any_language.ICodeCleaner;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.GithubSpringConfig;
import eu.solven.cleanthat.jgit.LocalFolderCodeProvider;
import eu.solven.cleanthat.language.java.JavaFormatter;
import io.sentry.IHub;

/**
 * The mojo doing actual cleaning
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = "cleanthat",
		defaultPhase = LifecyclePhase.PROCESS_SOURCES,
		threadSafe = true,
		// Used to enable symbolSolving based on project dependencies
		requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CleanThatCleanThatMojo extends ACleanThatMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatCleanThatMojo.class);

	protected static final AtomicReference<CleanThatCleanThatMojo> CURRENT_MOJO = new AtomicReference<>();

	/**
	 * The SpringBoot application started within maven Mojo
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	@SpringBootApplication(scanBasePackages = "none")
	@Import({ GithubSpringConfig.class, JavaFormatter.class, CodeProviderHelpers.class })
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
		getLog().info("Hello, world.");

		if (CURRENT_MOJO.compareAndSet(null, this)) {
			try {
				SpringApplication.run(MavenSpringConfig.class);
			} finally {
				LOGGER.info("Closed applicationContext");
				// Beware to clean so that it is OK in a multiModule reactor
				CURRENT_MOJO.set(null);
			}
		} else {
			throw new IllegalStateException("We have a leftover Mojo");
		}
	}

	public void doClean(ApplicationContext appContext) {
		String configPath = getConfigPath();
		getLog().info("Path: " + configPath);
		getLog().info("URL: " + getConfigUrl());

		// getLog().info("parentFile: " + getProject().getParentFile());
		// getLog().info("parent.file: " + getProject().getParent().getFile());

		Path configPathFile = Paths.get(configPath);

		File baseFir = getProject().getBasedir();

		Path configPathFileParent = configPathFile.getParent();
		if (!configPathFileParent.equals(baseFir.toPath())) {
			LOGGER.info("We'll clean only in a module containing the configuration: {}", configPathFileParent);
			return;
		}

		getLog().info("project.baseDir: " + baseFir);

		// Process the root of current module
		ICodeProviderWriter codeProvider = new LocalFolderCodeProvider(baseFir.toPath());

		ICodeCleaner codeCleaner = new MavenCodeCleaner(
				appContext.getBeansOfType(ObjectMapper.class).values().stream().collect(Collectors.toList()),
				appContext.getBean(ICodeProviderFormatter.class));

		codeCleaner.formatCodeGivenConfig(codeProvider, isDryRun());
	}
}
