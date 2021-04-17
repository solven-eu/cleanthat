package eu.solven.cleanthat.mvn;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

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
	public void execute() throws MojoExecutionException {
		getLog().info("Hello, world.");
		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());
	}
}