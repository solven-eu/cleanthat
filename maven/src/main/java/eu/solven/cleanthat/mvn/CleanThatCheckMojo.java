package eu.solven.cleanthat.mvn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.springframework.context.ApplicationContext;

import eu.solven.cleanthat.any_language.ICodeCleaner;
import eu.solven.cleanthat.code_provider.github.GithubSpringConfig;
import eu.solven.cleanthat.codeprovider.CodeProviderHelpers;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.formatter.CodeFormatResult;
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
		checkParameters();

		getLog().info("Path: " + getConfigPath());
		getLog().info("URL: " + getConfigUrl());

		ICodeProviderWriter codeProvider = CleanThatMavenHelper.makeCodeProviderWriter(this);
		ICodeCleaner codeCleaner = CleanThatMavenHelper.makeCodeCleaner(appContext);
		CodeFormatResult result = codeCleaner.formatCodeGivenConfig(codeProvider, true);

		if (!result.isEmpty()) {
			throw new MojoFailureException("ARG",
					"CleanThat would have impacted the code",
					"CleanThat would have impacted the code");
		}
	}
}