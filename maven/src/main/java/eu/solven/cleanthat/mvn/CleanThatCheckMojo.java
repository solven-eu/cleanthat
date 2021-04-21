package eu.solven.cleanthat.mvn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.formatter.CodeProviderFormatter;
import eu.solven.cleanthat.formatter.eclipse.JavaFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.jgit.LocalFolderCodeProvider;

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
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Hello, world.");
		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());

		ObjectMapper om = new ObjectMapper();

		CodeProviderFormatter codeProviderFormatter = new CodeProviderFormatter(om, new JavaFormatter(om));
		File pathToConfig = CodeProviderHelpers.pathToConfig(Paths.get("."));
		CleanthatRepositoryProperties properties;
		try {
			properties = om.readValue(pathToConfig, CleanthatRepositoryProperties.class);
		} catch (IOException e) {
			throw new IllegalArgumentException("Issue with configuration at " + pathToConfig, e);
		}
		codeProviderFormatter.formatCode(properties, new LocalFolderCodeProvider(Paths.get(".")));
	}
}