package eu.solven.cleanthat.mvn;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * The mojo checking the code is clean
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CleanThatCheckMojo extends ACleanThatMojo {
	// TODO Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html
	public void execute() throws MojoExecutionException {
		getLog().info("Hello, world.");
		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());
	}
}