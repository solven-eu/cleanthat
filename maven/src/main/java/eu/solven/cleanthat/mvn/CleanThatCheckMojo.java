package eu.solven.cleanthat.mvn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.code_provider.local.FileSystemCodeProvider;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.config.ConfigHelpers;
import eu.solven.cleanthat.formatter.ICodeProviderFormatter;
import eu.solven.cleanthat.github.CleanthatRepositoryProperties;
import eu.solven.cleanthat.lambda.AllLanguagesSpringConfig;

/**
 * The mojo checking the code is clean
 * 
 * @author Benoit Lacelle
 *
 */
// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
@Mojo(name = CleanThatCheckMojo.MOJO_CHECK, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CleanThatCheckMojo extends ACleanThatSpringMojo {
	public static final String MOJO_CHECK = "check";

	@Override
	protected List<Class<?>> springClasses() {
		List<Class<?>> classes = new ArrayList<>();

		classes.add(GithubSpringConfig.class);
		classes.add(AllLanguagesSpringConfig.class);
		classes.add(CodeProviderHelpers.class);

		return classes;
	}

	// TODO Inspire from https://maven.apache.org/plugins/maven-pmd-plugin/check-mojo.html
	@Override
	protected void doClean(ApplicationContext appContext) throws IOException, MojoFailureException {
		getLog().info("Hello, world.");
		checkParameters();

		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());

		Collection<ObjectMapper> oms = appContext.getBeansOfType(ObjectMapper.class).values();

		File pathToConfig = CodeProviderHelpers.pathToConfig(Paths.get("."));

		CleanthatRepositoryProperties properties =
				new ConfigHelpers(oms).loadRepoConfig(new FileSystemResource(pathToConfig));
		ICodeProviderFormatter codeProviderFormatter = appContext.getBean(ICodeProviderFormatter.class);
		codeProviderFormatter.formatCode(properties, new FileSystemCodeProvider(Paths.get(".")), false);
	}
}