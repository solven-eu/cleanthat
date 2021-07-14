package eu.solven.cleanthat.mvn;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
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
	// Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/pmd-mojo.html
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Hello, world.");
		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());

		// getSession().getBas.getProjects().get(0).getBasedir()B

		File baseFir = getProject().getBasedir();

		// SpringApplication.run(getClass(), null);

		// IGithubRefCleaner cleaner = new LocalCleaner();

		// Process the root of current module
		ICodeProviderWriter codeProvider = new LocalFolderCodeProvider(baseFir.toPath());

		String configPath = getConfigPath();

		// CodeProviderFormatter codeProviderFormatter = appContext.getBean(CodeProviderFormatter.class);
		// File pathToConfig = CodeProviderHelpers.pathToConfig(localFolder);
		// CleanthatRepositoryProperties properties =
		// appContext.getBean(ObjectMapper.class).readValue(pathToConfig, CleanthatRepositoryProperties.class);
	}
}