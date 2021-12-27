package eu.solven.cleanthat.mvn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import eu.solven.cleanthat.any_language.ICodeCleaner;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.lambda.AllLanguagesSpringConfig;

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
		requiresDependencyResolution = ResolutionScope.RUNTIME,
		// One may rely on the mvn plugin to clean a folder, even if no pom.xml is available
		requiresProject = false)
public class CleanThatCleanThatMojo extends ACleanThatSpringMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanThatCleanThatMojo.class);

	@Override
	protected List<Class<?>> springClasses() {
		List<Class<?>> classes = new ArrayList<>();

		classes.add(GithubSpringConfig.class);
		classes.add(AllLanguagesSpringConfig.class);
		classes.add(CodeProviderHelpers.class);

		return classes;
	}

	@Override
	public void doClean(ApplicationContext appContext) {
		if (isRunOnlyAtRoot() && !isThisTheExecutionRoot()) {
			// This will check it is called only if the command is run from the project root.
			// However, it will not prevent the plugin to be called on each module
			getLog().info("maven-cleanthat-plugin:cleanthat skipped (not project root)");
			return;
		}

		String configPath = getConfigPath();
		getLog().info("Path: " + configPath);
		getLog().info("URL: " + getConfigUrl());

		File baseDir = getBaseDir();

		Path configPathFile = Paths.get(configPath);
		Path configPathFileParent = configPathFile.getParent();
		getLog().info("configPathFileParent: " + configPathFileParent);

		if (!configPathFileParent.equals(baseDir.toPath())) {
			LOGGER.info("We'll clean only in a module containing the configuration: {}", configPathFileParent);
			return;
		}

		getLog().info("project.baseDir: " + baseDir);

		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);

		ICodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);

		codeCleaner.formatCodeGivenConfig(codeProvider, isDryRun());
	}
}
