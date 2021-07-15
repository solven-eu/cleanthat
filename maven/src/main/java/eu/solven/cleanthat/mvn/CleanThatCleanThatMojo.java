package eu.solven.cleanthat.mvn;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
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
import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.GithubSpringConfig;
import eu.solven.cleanthat.jgit.LocalFolderCodeProvider;

/**
 * The mojo doing actual cleaning
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = "cleanthat", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class CleanThatCleanThatMojo extends ACleanThatMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatCleanThatMojo.class);

	protected static final AtomicReference<CleanThatCleanThatMojo> CURRENT_MOJO = new AtomicReference<>();

	@SpringBootApplication(scanBasePackages = "none")
	@Import({ GithubSpringConfig.class, JavaFormatter.class, CodeProviderHelpers.class })
	public static class MavenSpringConfig implements CommandLineRunner {

		@Autowired
		ApplicationContext appContext;

		@Override
		public void run(String... args) throws Exception {
			LOGGER.info("Processing arguments: {}", Arrays.asList(args));

			CURRENT_MOJO.get().doClean(appContext);
		}

	}

	// Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Hello, world.");

		if (CURRENT_MOJO.compareAndSet(null, this)) {
			SpringApplication.run(MavenSpringConfig.class);
		} else {
			throw new IllegalStateException("We have a leftover Mojo");
		}
	}

	public void doClean(ApplicationContext appContext) {
		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());

		// getLog().info("parentFile: " + getProject().getParentFile());
		// getLog().info("parent.file: " + getProject().getParent().getFile());

		File baseFir = getProject().getBasedir();

		// Process the root of current module
		ICodeProviderWriter codeProvider = new LocalFolderCodeProvider(baseFir.toPath());

		String configPath = getConfigPath();

		ICodeCleaner codeCleaner = new MavenCodeCleaner(appContext.getBean(ObjectMapper.class),
				appContext.getBean(ICodeProviderFormatter.class));

		codeCleaner.formatCodeGivenConfig(codeProvider);
	}
}